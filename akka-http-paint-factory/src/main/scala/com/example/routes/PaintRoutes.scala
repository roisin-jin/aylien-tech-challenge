package com.example.routes

import java.sql.Timestamp
import java.time.{Instant, ZoneId, ZonedDateTime}

import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpChallenges
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.ask
import com.example.db.{ApiUser, ApiUserRequestRecord}
import com.example.service.DbRegistryActor._
import com.example.service.PaintWsActor.ApiUserRequest
import com.example.util._

import scala.concurrent.Future
import scala.util.Success

trait PaintRoutes extends BaseRoutes {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.example.util.CustomizedDirectives._
  import com.example.util.JsonFormats._
  import spray.json._

  val userCache: Cache[ApiCredential, AuthenticationResult[ApiUser]]
  val wsCache: Cache[String, String]

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

  // Create rate limit throttler
  lazy val requestedWithinRate = throttle(maxRequestsPerSecond)
  lazy val paintRoutes: Route = apiAuthenticateOrRejectWithChallenge(apiUserAuthenticator _)(user =>
    concat(
      pathPrefix("v1")(authorize(user.hasV1Access)(requestedWithinRate(v1Routes(user)))),
      pathPrefix("v2")(authorize(user.hasValidAccess)(requestedWithinRate(v2Routes(user))))
    ))

  def v1Routes(implicit user: ApiUser): Route = {
    val v1SuccessResp: String => Route = resp =>
      completeWithApiRequestRecordAdded(StatusCodes.OK, resp, (StatusCodes.OK, resp))

    get(parameters("input") { inputStr =>
      val paintRequest = inputStr.parseJson.convertTo[InternalRequest].getConvertedPaintRequest
      validateAndProcessPaintRequest(paintRequest, () => inputStr, v1SuccessResp)
    }) ~ post(entity(as[InternalRequest]) { inputEntity =>
      val paintRequest = inputEntity.getConvertedPaintRequest
      validateAndProcessPaintRequest(paintRequest, () => inputEntity.toJson.compactPrint, v1SuccessResp)
    })
  }

  // request history of user requests, sorted by most recent
  // page size set to 50 by default, returning 100 results at most
  // offset set to 0 by default
  def v2Routes(implicit user: ApiUser): Route =
    pathPrefix("history")(parameterMap { params =>
      val pageSize: Int = math.min(params.getOrElse("pageSize", "50").toInt, 100)
      val offSet: Int = params.getOrElse("offSet", "0").toInt
      val getHistoryRequest = GetUserRequestRecords(user.id, pageSize, offSet)
      val response = (dbRegistryActor ? getHistoryRequest).mapTo[GetUserRequestRecordsResponse]
      onSuccess(response)(result => complete((StatusCodes.OK, result)))
    }) ~
      (path("solve") & postSession) (entity(as[PaintRequest])(paintRequest =>
        validateAndProcessPaintRequest(paintRequest, () => paintRequest.getConvertedInternalRequest.toJson.compactPrint, _ match {
          case "IMPOSSIBLE" =>
            val code = PaintRequestValidater.errorCodeNoSolution
            completeWithApiRequestRecordAdded(code, code.reason, (code, code.reason))
          case result => {
            //parse response from python app
            val results = result.split(" ")
            val solutions: Seq[PaintDemand] = (1 to results.size) map (color => PaintDemand(color, results(color - 1).toInt))
            val paintRequestJsonStr = PaintResponse(solutions).toJson.prettyPrint
            val responseEntity = HttpEntity(contentType = ContentTypes.`application/json`, string = paintRequestJsonStr)
            completeWithApiRequestRecordAdded(StatusCodes.OK, paintRequestJsonStr, (StatusCodes.OK, responseEntity))
          }
        })))

  def validateAndProcessPaintRequest(paintRequest: PaintRequest, inputBuilder: () => String, onSuccessResponse: String => Route)(implicit user: ApiUser): Route = {
    //validate request first before further processing
    PaintRequestValidater.validate(paintRequest) map (errorCode =>
      completeWithApiRequestRecordAdded(errorCode, errorCode.reason, (errorCode, errorCode.reason))) getOrElse onSuccess {
      // cache ws result based on input string
      val input = inputBuilder()
      val requestIfNoCahcheFound = (paintWsActor ? ApiUserRequest(user.id, input)).mapTo[String]
      wsCache.getOrLoad(input, _ => requestIfNoCahcheFound)
    }(onSuccessResponse(_))
  }

  //Completes routes by having request cord persisted to DB
  def completeWithApiRequestRecordAdded(responseCode: StatusCode, responseMessage: String, m: => ToResponseMarshallable)(implicit user: ApiUser): Route = ctx => {
    val requestTime = ZonedDateTime.now.withZoneSameInstant(ZoneId.of("UTC")).toInstant
    generateUserRequestRecord(ctx.request, responseCode, responseMessage, requestTime) flatMap { apiUserRequestRecord =>
      dbRegistryActor ! CreateUserRequestRecord(apiUserRequestRecord)
      ctx.complete(m)
    }
  }

  def generateUserRequestRecord(httpRequest: HttpRequest, responseCode: StatusCode, responseMessage: String, requestTime: Instant)(implicit user: ApiUser) = {
    val futurePostDody: Future[Option[String]] = if (httpRequest.method == HttpMethods.POST) {
      Unmarshal(httpRequest.entity).to[String].map(Some(_))
    } else Future(None)

    futurePostDody map { body =>
      ApiUserRequestRecord(0, user.id, httpRequest.uri.toString, httpRequest.method.value, body, responseCode.value, responseMessage, Timestamp.from(requestTime))
    }
  }

}