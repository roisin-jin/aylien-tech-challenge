package com.example

import java.time.{ ZoneId, ZonedDateTime }

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.caching.scaladsl.CachingSettings
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpChallenges
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.example.PaintWsActor.Crash
import com.example.db.DbRegistryActor.{ CreateUser, CreateUserRequestRecord, GetAllUsers, GetUser, GetUserRequestRecords, GetUserRequestRecordsResponse, GetUserResponse }
import com.example.db.{ ApiUser, ApiUserRequestRecord }
import com.typesafe.config.Config

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

class PaintRoutes(dbRegistryActor: ActorRef, paintWsActor: ActorRef)(implicit system: ActorSystem) extends Throttle {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json._

  implicit val executionContext: ExecutionContext = system.dispatcher
  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("main-app.routes.ask-timeout"))
  private val maxRequestsPerSecond = system.settings.config.getInt("main-app.rate-limit.requests-per-second")
  private val superUser: Config = system.settings.config.getConfig("main-app.superUser")

  val challenge = HttpChallenges.basic("paintFactory")

  lazy val defaultCacheSettings = CachingSettings(system)
  lazy val userCache = ApiCacheSetting.generateUserCache(defaultCacheSettings)
  lazy val wsCache = ApiCacheSetting.generatePathCache(defaultCacheSettings)

  def findUser(creds: ApiCredential): Future[GetUserResponse] = (dbRegistryActor ? GetUser(creds)).mapTo[GetUserResponse]

  def apiUserAuthenticator(apiCreds: Option[ApiCredential]): Future[AuthenticationResult[ApiUser]] = apiCreds match {
    case Some(creds) => userCache.get(creds) getOrElse {
      val userResponse = findUser(creds)
      val authenticationResult = userResponse map (_.users.headOption.map(Right(_)).getOrElse(Left(challenge)))
      // Initiate cache if it's the first time seeing user if there's no error
      userResponse andThen {
        case Success(GetUserResponse(_, "SUCCESS")) => userCache.put(creds, authenticationResult.mapTo[AuthenticationResult[ApiUser]])
      }
      authenticationResult
    }
    case None => Future(Left(challenge))
  }

  def processPaintRequest(user: ApiUser, inputJsonStr: String): Future[String] = {
    val requestTime = ZonedDateTime.now.withZoneSameInstant(ZoneId.of("UTC")).toInstant.toEpochMilli
    val apiUserRequestRecord = ApiUserRequestRecord(None, user.id.get, inputJsonStr, requestTime)

    // Persist user request before sending request to the paintWs
    dbRegistryActor ? CreateUserRequestRecord(apiUserRequestRecord)
    (paintWsActor ? apiUserRequestRecord).mapTo[String]
  }

  def isSuperUser(user: ApiUser): Boolean = user.appId == superUser.getString("appId") && user.appKey == superUser.getString("appKey")

  val postSession = pathEnd & post
  // Create rate limit throttler
  val requestedWithinRate = throttle(maxRequestsPerSecond)

  val routes: Route = ApiSecurityDirectives.apiAuthenticateOrRejectWithChallenge(apiUserAuthenticator _)(user =>
    requestedWithinRate(concat(
      pathPrefix("v1")(authorize(user.hasV1Access)(
        parameters("input")(input => get {
          val paintRequest = input.parseJson.convertTo[InternalRequest].getConvertedPaintRequest
          //Validate request format before further processing
          PaintRequestValidation.validate(paintRequest) map (errorCode => complete(errorCode)) getOrElse {
            onSuccess(processPaintRequest(user, input))(result => result match {
              case "IMPOSSIBLE" => complete(PaintRequestValidation.errorCodeNoSolution)
              case _ => complete((StatusCodes.OK, result))
            })
          }
        }))),
      pathPrefix("v2")(
        concat(
          postSession(authorize(user.hasValidAccess)(
            entity(as[PaintRequest])(request =>
              PaintRequestValidation.validate(request) map (errorCode => complete(errorCode)) getOrElse {
                val paintRequest = request.getConvertedInternalRequest.toJson.compactPrint
                onSuccess(processPaintRequest(user, paintRequest))(result => result match {
                  case "IMPOSSIBLE" => complete(PaintRequestValidation.errorCodeNoSolution)
                  case _ => complete((StatusCodes.OK, result))
                })
              }))),
          path("history")(get(parameterMap { params =>
            val size = params.getOrElse("pageSize", "50")
            val offSet = params.getOrElse("offSet", "0")
            val getHistoryRequest = GetUserRequestRecords(user.id.get, size.toInt, offSet.toInt)
            val response = (dbRegistryActor ? getHistoryRequest).mapTo[GetUserRequestRecordsResponse]
            onSuccess(response)(result => complete((StatusCodes.OK, result.records)))
          }))
        )),
      path("admin")(authorize(isSuperUser(user))(
        concat(
          path("crash")(onSuccess((paintWsActor ? Crash).mapTo[String])(msg => complete((StatusCodes.OK, msg)))),
          path("users")(get {
            val getUsers = (dbRegistryActor ? GetAllUsers).mapTo[GetUserResponse]
            onSuccess(getUsers) { resp =>
              val status = if (resp.message == "SUCCESS") StatusCodes.OK else StatusCodes.InternalServerError
              complete((status, resp))
            }
          }),
          path("user")(postSession(entity(as[ApiUser]) { newApiUser =>
            val createUserResponse = (dbRegistryActor ? CreateUser(newApiUser)).mapTo[String]
            onSuccess(createUserResponse)(msg => msg match {
              case "SUCCESS" => complete(StatusCodes.Created)
              case _ => complete((StatusCodes.BadRequest, msg))
            })
          }))
        )
      ))
    )))
}
