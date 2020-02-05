package com.example.routes

import java.sql.Timestamp
import java.time.{ZoneId, ZonedDateTime}

import akka.actor.ActorSystem
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives.AuthenticationResult
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestProbe
import com.example.db.{ApiUser, ApiUserRequestRecord}
import com.example.service.DbRegistryActor.CreateUserRequestRecord
import com.example.util.{ApiCredential, PaintRequestValidater}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class PaintRoutesSpec extends WordSpec with PaintRoutes
  with MockitoSugar with Matchers with ScalaFutures with ScalatestRouteTest {

  import com.example.util.CustomizedDirectives.{API_APP_ID_HEADER, API_APP_KEY_HEADER}

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(new DurationInt(10).second)

  val testUser = mock[ApiUser]
  when(testUser.id).thenReturn(1l)
  when(testUser.hasExpired).thenReturn(false)
  when(testUser.hasV1Access).thenReturn(true)

  val mockApiUserRequestRecord = mock[ApiUserRequestRecord]
  val mockUserRequest = mock[CreateUserRequestRecord]
  when(mockUserRequest.apiUserRequestRecord).thenReturn(mockApiUserRequestRecord)

  val dbRegistryActorProb = TestProbe("TestDbRegistryActor")
  val dbRegistryActor = dbRegistryActorProb.ref
  val paintWsActorProb = TestProbe("paintWsActor")
  val paintWsActor = paintWsActorProb.ref

  val userCache = mock[Cache[ApiCredential, AuthenticationResult[ApiUser]]]
  when(userCache.get(any[ApiCredential])).thenReturn(Some(Future(Right(testUser))))

  val wsCache = mock[Cache[String, RouteResult]]
  when(wsCache.get(any[String])).thenReturn(None)

  val routes = Route.seal(paintRoutes)

  val wrapUpWithUserHeaders: HttpRequest => HttpRequest = _.withHeaders(Seq(
    RawHeader(API_APP_ID_HEADER, "testUserId"),
    RawHeader(API_APP_KEY_HEADER, "testUserKey")))

  override def generateUserRequestRecord(user: ApiUser, inputJsonStr: String) = mockUserRequest

  "PainRoutes V1" should {
    "return IMPOSSIBLE if no present (GET /v1/?input) with unsovlable request" in {
      // note that there's no need for the host part in the uri:
      val input = """{"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}"""
      val request = Get(uri = s"/v1/?input=$input")
      val apiUserRequestRecord = mock[ApiUserRequestRecord]
      val createUserRequestRecord = CreateUserRequestRecord(apiUserRequestRecord)

      val result = wrapUpWithUserHeaders(request) ~> routes ~> runRoute
      dbRegistryActorProb.expectMsg(10.seconds, mockUserRequest)
      paintWsActorProb.expectMsg(10.seconds, mockApiUserRequestRecord)
      paintWsActorProb.reply("IMPOSSIBLE")

      check {
        status should ===(PaintRequestValidater.errorCodeNoSolution)
      } (result)
    }

    "be able to remove users (DELETE /users)" in {
      // user the RequestBuilding DSL provided by ScalatestRouteSpec:
      val request = Delete(uri = "/users/Kapi")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"description":"User Kapi deleted."}""")
      }
    }

  }

}


