package com.example.routes

import java.sql.Timestamp
import java.time.Instant

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestProbe
import com.example.db.{ApiUser, TestDbRegistryActor}
import com.example.service.PaintWsActor.Crash
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class SuperAdminRoutesSpec extends WordSpec with SuperAdminRoutes with Matchers with ScalaFutures with ScalatestRouteTest {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.example.util.CustomizedDirectives._
  import com.example.util.JsonFormats._
  import spray.json._

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(new DurationInt(10).second)

  val dbRegistryActor = system.actorOf(Props(new TestDbRegistryActor),"testDbRegistryActor")
  val paintWsActorProb = TestProbe("paintWsActor")
  val paintWsActor = paintWsActorProb.ref
  val testroutes: Route = Route.seal(adminRoutes)

  val wrapUpWithAdminHeaders: HttpRequest => HttpRequest = _.withHeaders(Seq(
    RawHeader(API_APP_ID_HEADER, "testAdminId"),
    RawHeader(API_APP_KEY_HEADER, "testAdminKey")))

  "AdminRoutes" should {

    "return no users if no present (GET /admin/users)" in {
      // note that there's no need for the host part in the uri:

      wrapUpWithAdminHeaders(Get("/admin/users")) ~> testroutes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"users":[]}""".parseJson.prettyPrint)
      }
    }

    "be able to add users (POST /user)" in {
      val user = ApiUser(0, "testAppId", "testAppKey", "test@email.com", false, false, Timestamp.from(Instant.now()))
      val userEntity = Marshal(user).to[MessageEntity].futureValue // futureValue is from ScalaFutures
      val request = wrapUpWithAdminHeaders(Post("/admin/user").withEntity(userEntity))

      request ~> testroutes ~> check {
        status should ===(StatusCodes.Created)
      }
    }

    "trigger crash event to paint ws" in {

      val result = wrapUpWithAdminHeaders(Get("/admin/crash")) ~> testroutes ~> runRoute
      paintWsActorProb.expectMsg(100 millis, Crash)

      check {
        status should ===(StatusCodes.Accepted)
      }(result)

    }

  }

}