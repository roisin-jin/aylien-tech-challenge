package com.example

import java.sql.Timestamp
import java.time.Instant

import akka.actor.Props
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, MessageEntity, StatusCodes}
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.example.db.{ApiUser, TestDbConfig}
import com.example.service.{DbRegistryActor, PaintWsActor}
import com.example.util.ApiCredential
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class PaintRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.example.util.CustomizedDirectives._
  import com.example.util.JsonFormats._

//  object TestDbRegistryActor extends DbRegistryActor with TestDbConfig {}
//
//  val testDbRegistry = system.actorOf(Props(TestDbRegistryActor),"TestDbRegistryActor")
//  val paintWsActor = system.actorOf(Props(new PaintWsActor()), "paintWsActor")
//  val userCache = mock[Cache[ApiCredential, AuthenticationResult[ApiUser]]]
//  val wsCache = mock[Cache[String, RouteResult]]
//  val routes = new PaintRoutes(testDbRegistry, paintWsActor, userCache, wsCache).routes

  // use the json formats to marshal and unmarshall objects in the test
//
//  "AdminRoutes" should {
//    "return no users if no present (GET /admin/users)" in {
//      // note that there's no need for the host part in the uri:
//      val request = HttpRequest(uri = "/admin/users", headers = Seq(
//        RawHeader(API_APP_ID_HEADER, "testAdminId"),
//        RawHeader(API_APP_KEY_HEADER, "testAdminKey")))
//
//      request ~> routes ~> check {
//        status should ===(StatusCodes.OK)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and no entries should be in the list:
//        entityAs[String] should ===("""{"users":[]}""")
//      }
//    }
//
//    "be able to add users (POST /users)" in {
//      val user = ApiUser(0, "testAppId", "testAppKey", "test@email.com", false, false, Timestamp.from(Instant.now()))
//      val userEntity = Marshal(user).to[MessageEntity].futureValue // futureValue is from ScalaFutures
//
//      // using the RequestBuilding DSL:
//      val request = Post("/users").withEntity(userEntity)
//
//      request ~> routes ~> check {
//        status should ===(StatusCodes.Created)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and we know what message we're expecting back:
//        entityAs[String] should ===("""{"description":"User Kapi created."}""")
//      }
//    }
//
//
//    "be able to remove users (DELETE /users)" in {
//      // user the RequestBuilding DSL provided by ScalatestRouteSpec:
//      val request = Delete(uri = "/users/Kapi")
//
//      request ~> routes ~> check {
//        status should ===(StatusCodes.OK)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and no entries should be in the list:
//        entityAs[String] should ===("""{"description":"User Kapi deleted."}""")
//      }
//    }
//
//  }

}


