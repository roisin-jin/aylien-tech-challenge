package com.example

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.headers.HttpChallenges
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.example.db.DbRegistryActor.GetUser
import com.example.db.{ ApiUser, DbRegistryActor }

import scala.concurrent.{ ExecutionContext, Future }

class PaintRoutes(dbRegistryActor: ActorRef[DbRegistryActor.Command])(implicit val system: ActorSystem[_]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import spray.json._

  implicit val executionContext: ExecutionContext = system.executionContext
  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("main-app.routes.ask-timeout"))

  val challenge = HttpChallenges.basic("paintFactory")

  def authenticate(creds: ApiCredential): Future[Option[ApiUser]] = dbRegistryActor ? (GetUser(creds, _))
  
  def apiUserAuthenticator(apiCreds: Option[ApiCredential]): Future[AuthenticationResult[ApiUser]] = apiCreds match {
    case Some(creds) => authenticate(creds) map (_.map(Right(_)).getOrElse(Left(challenge)))
    case None => Future(Left(challenge))
  }

  def getPaintResult: Future[String] = Future("")

  def hasValidAccess(user: ApiUser): Boolean = user.hasExpired

  val postSession = pathEnd & post

  val routes: Route = ApiSecurityDirectives.authenticateOrRejectWithChallenge(apiUserAuthenticator _)(user =>
    concat(
      pathPrefix("v1")(authorize(user.hasV1Access)(
        parameters("input")(input => get {
          val paintRequest = input.parseJson.convertTo[InternalRequest].getConvertedPaintRequest
          PaintRequestValidation.validate(paintRequest) map (errorCode =>
            complete(errorCode)) getOrElse {
            complete(dbRegistryActor ? GetUsers)
          }
        }))),
      pathPrefix("v2")(authorize(hasValidAccess(user))(
        concat(
          postSession(entity(as[PaintRequest])(request =>
            PaintRequestValidation.validate(request) map (errorCode =>
              complete(errorCode)) getOrElse {
              complete(dbRegistryActor ? GetUsers)
            })),
          path("history")(get(complete(dbRegistryActor ? GetUsers)))
        ))
      )
    ))
}
