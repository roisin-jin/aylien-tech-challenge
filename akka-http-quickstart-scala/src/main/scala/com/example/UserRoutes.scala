package com.example

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{ HttpChallenges, HttpCredentials }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
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
  def auth(apiKey: HttpCredentials): Boolean = true
  def apiUserAuthenticator(apiCreds: Option[HttpCredentials]): Future[AuthenticationResult[String]] =
    Future(apiCreds match {
        case Some(creds) if auth(creds) => Right("some-user-name-from-creds")
        case _ => Left(challenge)
    })

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)
  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))
  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))
  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))

  val userRoutes: Route = authenticateOrRejectWithChallenge(apiUserAuthenticator _)( _ =>
      pathPrefix("users")(
        concat(
          pathEnd {
            concat(
              get(complete(getUsers())),
              post {
                entity(as[User]) { user =>
                  onSuccess(createUser(user)) { performed =>
                    complete((StatusCodes.Created, performed))
                  }
                }
              })
          },
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
          }))
    )
}
