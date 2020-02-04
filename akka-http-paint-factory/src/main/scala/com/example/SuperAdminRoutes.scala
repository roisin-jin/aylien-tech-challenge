package com.example

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import com.example.db.ApiUser
import com.example.service.DbRegistryActor.{CreateUser, GetAllUsers, GetUserResponse}
import com.example.service.PaintWsActor.Crash
import com.example.util.{ApiCredential, BaseRoutes}
import com.typesafe.config.Config

trait SuperAdminRoutes extends BaseRoutes {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.example.util.CustomizedDirectives._
  import com.example.util.JsonFormats._
  import spray.json._

  private lazy val superUserConf: Config = system.settings.config.getConfig("main-app.superUser")

  def isSuperUser(apiCreds: Option[ApiCredential]): Boolean = apiCreds match {
    case Some(creds) => creds.appId == superUserConf.getString("appId") &&
      creds.appKey == superUserConf.getString("appKey")
    case None => false
  }

  lazy val getUsers = (dbRegistryActor ? GetAllUsers).mapTo[GetUserResponse]

  lazy val adminRoutes: Route =
    pathPrefix("admin")(extractApiCredentials(creds => authorize(isSuperUser(creds))(concat(
      path("crash") {
        paintWsActor ! Crash
        complete((StatusCodes.Accepted, "Crash event created"))
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
    ))
  ))

}
