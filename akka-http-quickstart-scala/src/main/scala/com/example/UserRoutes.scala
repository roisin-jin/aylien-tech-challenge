package com.example

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.{ DateTime, StatusCodes }
import akka.http.scaladsl.model.headers.{ HttpChallenge, HttpChallenges, HttpCredentials }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Route, StandardRoute }
import akka.util.Timeout
import com.example.UserRegistry._

import scala.concurrent.{ ExecutionContext, Future }

class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  implicit val executionContext: ExecutionContext = system.executionContext
  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  val challenge = HttpChallenges.basic("apiRealm")

  def auth(apiKey: HttpCredentials): AuthenticationResult[ApiUser] = Right(ApiUser(apiKey.token, DateTime.now.clicks, false))
  def apiUserAuthenticator(apiCreds: Option[HttpCredentials]): Future[AuthenticationResult[ApiUser]] =
    Future(apiCreds map auth getOrElse Left(challenge))

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)

  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))

  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))

  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))

  def hasValidAccess(user: ApiUser): Boolean = user.isExpired

  val postSession = pathEnd & post

  val routes: Route = authenticateOrRejectWithChallenge(apiUserAuthenticator _)(user =>
    concat(
      path("v1")(authorize(user.hasV1Access){
        get(complete(getUsers()))
      }),
      pathPrefix("v2")(authorize(hasValidAccess(user))(
          concat(
            postSession(entity(as[PaintRequest]) { request =>
              complete((StatusCodes.Created, requests.length))
            }),
            path(Segment) { name =>
              concat(
                get {

                  rejectEmptyResponse {
                    onSuccess(getUser(name)) { response =>
                      complete(response.maybeUser)
                    }
                  }

                },
                delete {
                  onSuccess(deleteUser(name)) { performed =>
                    complete((StatusCodes.OK, performed))
                  }

                })
            },
            path("history")(authorize(user.hasV1Access){
              get(complete(getUsers()))
            })
          ))
      )
    ))
}
