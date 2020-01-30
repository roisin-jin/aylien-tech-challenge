package com.example

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.headers.{ HttpChallenges, HttpCredentials }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.example.UserRegistry._
import com.example.db.ApiUser

import scala.concurrent.{ ExecutionContext, Future }

class PaintRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json._

  implicit val executionContext: ExecutionContext = system.executionContext
  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("main-app.routes.ask-timeout"))

  val challenge = HttpChallenges.basic("apiRealm")

  def auth(apiKey: HttpCredentials): AuthenticationResult[ApiUser] = Right(ApiUser(Some(1L), apiKey.token, "company", true, false))
  def apiUserAuthenticator(apiCreds: Option[HttpCredentials]): Future[AuthenticationResult[ApiUser]] =
    Future(apiCreds map auth getOrElse Left(challenge))

  def getPaintResult: Future[String] = Future("")

  def hasValidAccess(user: ApiUser): Boolean = user.isEnabled

  val postSession = pathEnd & post

  val routes: Route = authenticateOrRejectWithChallenge(apiUserAuthenticator _)(user =>
    concat(
      pathPrefix("v1")(authorize(user.hasV1Access)(
        parameters("input")(input => get {
          val paintRequest = input.parseJson.convertTo[InternalRequest].getConvertedPaintRequest
          PaintRequestValidation.validate(paintRequest) map (errorCode =>
            complete(errorCode)) getOrElse {
            complete(userRegistry ? GetUsers)
          }
        }))),
      pathPrefix("v2")(authorize(hasValidAccess(user))(
          concat(
            postSession(entity(as[PaintRequest])(request =>
              PaintRequestValidation.validate(request) map (errorCode =>
                complete(errorCode)) getOrElse {
                complete(userRegistry ? GetUsers)
              })),
            path("history")(get(complete(userRegistry ? GetUsers)))
          ))
      )
    ))
}
