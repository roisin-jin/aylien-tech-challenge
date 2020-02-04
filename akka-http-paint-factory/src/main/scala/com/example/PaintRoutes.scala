package com.example

import java.sql.Timestamp
import java.time.{ZoneId, ZonedDateTime}

import akka.actor.{ActorRef, ActorSystem}
import akka.http.caching.scaladsl.CachingSettings
import akka.http.scaladsl.model.headers.HttpChallenges
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.CachingDirectives._
import akka.pattern.ask
import akka.util.Timeout
import com.example.PaintWsActor.Crash
import com.example.db.DbRegistryActor._
import com.example.db.{ApiUser, ApiUserRequestRecord}
import com.example.util._
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class PaintRoutes(dbRegistryActor: ActorRef, paintWsActor: ActorRef)(implicit system: ActorSystem) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.example.util.CustomizedDirectives._
  import com.example.util.JsonFormats._
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
    val requestTime = ZonedDateTime.now.withZoneSameInstant(ZoneId.of("UTC")).toInstant
    val apiUserRequestRecord = ApiUserRequestRecord(0, user.id, inputJsonStr, Timestamp.from(requestTime))

    // Persist user request before sending request to the paintWs
    dbRegistryActor ? CreateUserRequestRecord(apiUserRequestRecord)
    (paintWsActor ? apiUserRequestRecord).mapTo[String]
  }

  // Create rate limit throttler
  val requestedWithinRate = throttle(maxRequestsPerSecond)
  val rootRoutes: Route = apiAuthenticateOrRejectWithChallenge(apiUserAuthenticator _)(user => concat(
    v1Routes(user),
    requestedWithinRate(v2Routes(user)),
    adminRoutes(user)))

  val v1Routes: ApiUser => Route = user => pathPrefix("v1")(authorize(user.hasV1Access)(
    parameters("input")(input => cache(wsCache, _ => input)(requestedWithinRate(get {
      val paintRequest = input.parseJson.convertTo[InternalRequest].getConvertedPaintRequest
      //Validate request format before further processing
      PaintRequestValidater.validate(paintRequest) map (errorCode =>
        complete(errorCode)) getOrElse onSuccess(processPaintRequest(user, input))(result => result match {
        case "IMPOSSIBLE" => complete(PaintRequestValidater.errorCodeNoSolution)
        case _ => complete(HttpResponse(entity = result, status = StatusCodes.OK))
      })
    })))
  ))

  val postSession = pathEnd & post
  val v2Routes: ApiUser => Route = user => concat(
    postSession(authorize(user.hasValidAccess)(
      entity(as[PaintRequest])(request =>
        PaintRequestValidater.validate(request) map (errorCode => complete(errorCode)) getOrElse {
          val input = request.getConvertedInternalRequest.toJson.compactPrint
          cache(wsCache, _ => input)(onSuccess(processPaintRequest(user, input))(result => result match {
            case "IMPOSSIBLE" => complete(PaintRequestValidater.errorCodeNoSolution)
            case _ => {
              //parse response from python app
              val results = result.split(" ")
              val solutions: Seq[PaintDemand] = (1 to results.size) map (color => PaintDemand(color, results(color).toInt))
              val responseEntity = HttpEntity(contentType = ContentTypes.`application/json`, string = PaintResponse(solutions).toJson.prettyPrint)
              complete(HttpResponse(entity = responseEntity, status = StatusCodes.OK))
            }
          }))
        }))),
    path("history")(get(parameterMap { params =>
      val size = params.getOrElse("pageSize", "50")
      val offSet = params.getOrElse("offSet", "0")
      val getHistoryRequest = GetUserRequestRecords(user.id, size.toInt, offSet.toInt)
      val response = (dbRegistryActor ? getHistoryRequest).mapTo[GetUserRequestRecordsResponse]
      onSuccess(response)(result => complete((StatusCodes.OK, result.records)))
    })))


  val isSuperUser: ApiUser => Boolean = user =>
    user.appId == superUser.getString("appId") && user.appKey == superUser.getString("appKey")
  val adminRoutes: ApiUser => Route = user => path("admin")(authorize(isSuperUser(user))(concat(
    path("crash") {
      paintWsActor ! Crash
      complete((StatusCodes.OK, "Crash event created"))
    },
    path("users")(get {
      val getUsers = (dbRegistryActor ? GetAllUsers).mapTo[GetUserResponse]
      onSuccess(getUsers) { resp =>
        if (resp.message == "SUCCESS") {
          val responseEntity = HttpEntity(contentType = ContentTypes.`application/json`, string = resp.toJson.prettyPrint)
          complete(HttpResponse(entity = responseEntity, status = StatusCodes.OK))
        } else complete(StatusCodes.InternalServerError)
      }
    }),
    path("user")(postSession(entity(as[ApiUser]) { newApiUser =>
      val createUserResponse = (dbRegistryActor ? CreateUser(newApiUser)).mapTo[String]
      onSuccess(createUserResponse)(msg => msg match {
        case "SUCCESS" => complete(StatusCodes.Created)
        case _ => complete((StatusCodes.BadRequest, msg))
      })
    }))
  )))
}
