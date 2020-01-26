package com.example

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.{ ExecutionContext, Future }
import com.example.UserRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.directives.Credentials
import akka.util.Timeout

//#import-json-formats
//#user-routes-class
class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {

  //#user-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#import-json-formats
  implicit val executionContext: ExecutionContext = system.executionContext
  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def requestUserAuthenticator(credentials: Credentials): Future[Option[String]] =
    credentials match {
      case p:Credentials.Provided =>
        Future {
          // potentially
          if (p.verify("p4ssw0rd")) Some(p.identifier)
          else None
        }
      case _ => Future.successful(None)
    }

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)
  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))
  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))
  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))

  //#all-routes
  //#users-get-post
  //#users-get-delete
  val userRoutes: Route = Route.seal(
    pathPrefix("users")(authenticateBasicAsync("realm", requestUserAuthenticator)( _ =>
      concat(
          //#users-get-delete
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
          //#users-get-delete
          //#users-get-post
          path(Segment) { name =>
            concat(
              get {
                //#retrieve-user-info
                rejectEmptyResponse {
                  onSuccess(getUser(name)) { response =>
                    complete(response.maybeUser)
                  }
                }
                //#retrieve-user-info
              },
              delete {
                //#users-delete-logic
                onSuccess(deleteUser(name)) { performed =>
                  complete((StatusCodes.OK, performed))
                }
                //#users-delete-logic
              })
          }))
      //#users-get-delete
    ))
  //#all-routes
}
