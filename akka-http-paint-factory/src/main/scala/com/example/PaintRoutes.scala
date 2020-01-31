package com.example

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.caching.scaladsl.CachingSettings
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpChallenges
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.example.db.DbRegistryActor.{ CreateUser, GetUser, GetUserResponse }
import com.example.db.{ ApiUser, ApiUserRequestRecord }
import com.typesafe.config.Config

import scala.concurrent.{ ExecutionContext, Future }

class PaintRoutes(dbRegistryActor: ActorRef, paintWsActor: ActorRef)(implicit system: ActorSystem) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json._

  implicit val executionContext: ExecutionContext = system.dispatcher
  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("main-app.routes.ask-timeout"))
  private val superUser: Config = system.settings.config.getConfig("superUser")

  val challenge = HttpChallenges.basic("paintFactory")

  lazy val defaultCacheSettings = CachingSettings(system)
  lazy val userCache = ApiCacheSetting.generateUserCache(defaultCacheSettings)
  lazy val wsCache = ApiCacheSetting.generatePathCache(defaultCacheSettings)

  def findUser(creds: ApiCredential): Future[GetUserResponse] = (dbRegistryActor ? GetUser(creds)).mapTo[GetUserResponse]
  def apiUserAuthenticator(apiCreds: Option[ApiCredential]): Future[AuthenticationResult[ApiUser]] = apiCreds match {
    case Some(creds) => userCache.get(creds) getOrElse {
      val authenticationResult = findUser(creds) map (_.user.map(Right(_)).getOrElse(Left(challenge)))
      // Initate cache if it's the first time seeing user
      userCache.put(creds, authenticationResult.mapTo[AuthenticationResult[ApiUser]])
      authenticationResult
    }
    case None => Future(Left(challenge))
  }

  def getPaintResult(apiUserRequestRecord: ApiUserRequestRecord): Future[String] = {
    (paintWsActor ? apiUserRequestRecord).mapTo[String]
  }

  def isSuperUser(user: ApiUser): Boolean = user.appId == superUser.getString("appId") && user.appKey == superUser.getString("appKey")

  val postSession = pathEnd & post

  val routes: Route = ApiSecurityDirectives.apiAuthenticateOrRejectWithChallenge(apiUserAuthenticator _)(user =>
    concat(
      pathPrefix("v1")(authorize(user.hasV1Access)(
        parameters("input")(input => get {
          val paintRequest = input.parseJson.convertTo[InternalRequest].getConvertedPaintRequest
          PaintRequestValidation.validate(paintRequest) map (errorCode =>
            complete(errorCode)) getOrElse {
            complete(StatusCodes.OK)
          }
        }))),
      pathPrefix("v2")(
        concat(
          postSession(authorize(user.hasValidAccess)(
            entity(as[PaintRequest])(request =>
              PaintRequestValidation.validate(request) map (errorCode =>
                complete(errorCode)) getOrElse {
                val input = request.getConvertedInternalRequest.toJson.toString()
                val apiUserRequestRecord = ApiUserRequestRecord(None, user.id.get, input, 1L)
                (paintWsActor ? apiUserRequestRecord)
                complete(StatusCodes.OK)
              }))),
          path("history")(get(complete(StatusCodes.OK)))
        )),
      path("admin")(authorize(isSuperUser(user))(
        concat(
          path("crash")(onSuccess(getPaintResult(""))(msg =>
            complete((StatusCodes.OK, msg)))),
          path("user")(postSession(entity(as[ApiUser])(newApiUser =>
            onSuccess((dbRegistryActor ? CreateUser(newApiUser)).mapTo[String])(msg =>
              complete((StatusCodes.Created, msg)))
          )))
        )
      ))
    ))
}
