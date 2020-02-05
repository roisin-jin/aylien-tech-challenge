package com.example

import java.sql.Timestamp
import java.time.{ZoneId, ZonedDateTime}

import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpChallenges
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.CachingDirectives._
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.pattern.ask
import com.example.db.{ApiUser, ApiUserRequestRecord}
import com.example.service.DbRegistryActor._
import com.example.util._

import scala.concurrent.Future
import scala.util.Success

trait PaintRoutes extends BaseRoutes {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.example.util.CustomizedDirectives._
  import com.example.util.JsonFormats._
  import spray.json._

  val userCache: Cache[ApiCredential, AuthenticationResult[ApiUser]]
  val wsCache: Cache[String, RouteResult]

  private lazy val maxRequestsPerSecond = system.settings.config.getInt("main-app.rate-limit.requests-per-second")

  val challenge = HttpChallenges.basic("paintFactory")

  def findUser(creds: ApiCredential): Future[GetUserResponse] = (dbRegistryActor ? GetUser(creds)).mapTo[GetUserResponse]

  def apiUserAuthenticator(apiCreds: Option[ApiCredential]): Future[AuthenticationResult[ApiUser]] = apiCreds match {
    case Some(creds) => userCache.get(creds) getOrElse {
      val userResponse = findUser(creds)
      val authenticationResult = userResponse map (_.users.headOption.map(Right(_)).getOrElse(Left(challenge)))
      // Initiate cache if it's the first time seeing user if there's no error
      userResponse andThen {
        case Success(GetUserResponse(_)) => userCache.put(creds, authenticationResult.mapTo[AuthenticationResult[ApiUser]])
      }
      authenticationResult
    }
    case None => Future(Left(challenge))
  }

  def processPaintRequest(user: ApiUser, inputJsonStr: String): Future[String] = {
    val requestTime = ZonedDateTime.now.withZoneSameInstant(ZoneId.of("UTC")).toInstant
    val apiUserRequestRecord = ApiUserRequestRecord(0, user.id, inputJsonStr, Timestamp.from(requestTime))

    // Persist user request before sending request to the paintWs
    dbRegistryActor ! CreateUserRequestRecord(apiUserRequestRecord)
    (paintWsActor ? apiUserRequestRecord).mapTo[String]
  }

  // Create rate limit throttler
  lazy val requestedWithinRate = throttle(maxRequestsPerSecond)
  lazy val paintRoutes: Route = apiAuthenticateOrRejectWithChallenge(apiUserAuthenticator _)(user => concat(
    pathPrefix("v1")(authorize(user.hasV1Access)(requestedWithinRate(v1Routes(user)))),
    pathPrefix("v2")(authorize(user.hasValidAccess)(requestedWithinRate(v2Routes(user))))
  ))

  // cache v1 result based on input string
  lazy val v1Routes: ApiUser => Route = user => parameters("input")(input =>
    cache(wsCache, _ => input) {
      val paintRequest = input.parseJson.convertTo[InternalRequest].getConvertedPaintRequest
      //Validate request format before further processing
      PaintRequestValidater.validate(paintRequest) map (errorCode =>
        complete(errorCode)) getOrElse onSuccess(processPaintRequest(user, input))(result => result match {
        case "IMPOSSIBLE" => complete(PaintRequestValidater.errorCodeNoSolution)
        case _ => complete(HttpResponse(entity = result, status = StatusCodes.OK))
      })
    })

  lazy val v2Routes: ApiUser => Route = user =>
    // request history of user requests, sorted by most recent
    // page size set to 50 by default, returning 100 results at most
    // offset set to 0 by default
    path("history")(parameterMap { params =>
      val pageSize: Int = math.min(params.getOrElse("pageSize", "50").toInt, 100)
      val offSet: Int = params.getOrElse("offSet", "0").toInt
      cache(wsCache, _ => s"history?userId=${user.id}&pageSize=$pageSize&offSet=$offSet") {
        val getHistoryRequest = GetUserRequestRecords(user.id, pageSize, offSet)
        val response = (dbRegistryActor ? getHistoryRequest).mapTo[GetUserRequestRecordsResponse]
        onSuccess(response)(result => complete((StatusCodes.OK, result.records)))
      }
    }) ~
      postSession(requestedWithinRate(
        entity(as[PaintRequest])(request =>
          PaintRequestValidater.validate(request) map (errorCode => complete(errorCode)) getOrElse {
            val input = request.getConvertedInternalRequest.toJson.compactPrint
            cache(wsCache, _ => input)(
              onSuccess(processPaintRequest(user, input)) {
                case "IMPOSSIBLE" => complete(PaintRequestValidater.errorCodeNoSolution)
                case result => {
                  //parse response from python app
                  val results = result.split(" ")
                  val solutions: Seq[PaintDemand] = (1 to results.size) map (color => PaintDemand(color, results(color).toInt))
                  val responseEntity = HttpEntity(contentType = ContentTypes.`application/json`, string = PaintResponse(solutions).toJson.prettyPrint)
                  complete(HttpResponse(entity = responseEntity, status = StatusCodes.OK))
                }
              })
          })))
}
