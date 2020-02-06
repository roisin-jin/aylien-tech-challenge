package com.example.routes

import java.time.Instant

import akka.actor.ActorSystem
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.AuthenticationResult
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestProbe
import com.example.db.{ApiUser, ApiUserRequestRecord}
import com.example.service.DbRegistryActor.{CreateUserRequestRecord, GetUserRequestRecords, GetUserRequestRecordsResponse}
import com.example.service.PaintWsActor.ApiUserRequest
import com.example.util._
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

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.example.util.CustomizedDirectives.{API_APP_ID_HEADER, API_APP_KEY_HEADER}
  import com.example.util.JsonFormats._
  import spray.json._

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(new DurationInt(10).second)

  val testUser = mock[ApiUser]
  when(testUser.id).thenReturn(1l)
  when(testUser.hasValidAccess).thenReturn(true)
  when(testUser.hasV1Access).thenReturn(true)

  val dbRegistryActorProb = TestProbe("TestDbRegistryActor")
  val dbRegistryActor = dbRegistryActorProb.ref
  val paintWsActorProb = TestProbe("paintWsActor")
  val paintWsActor = paintWsActorProb.ref

  val userCache = mock[Cache[ApiCredential, AuthenticationResult[ApiUser]]]
  when(userCache.get(any[ApiCredential])).thenReturn(Some(Future(Right(testUser))))

  val wsCache = ApiCacheSetting.generateWsResultCache(CachingSettings(system))
  val routes = Route.seal(paintRoutes)

  val wrapUpWithUserHeaders: HttpRequest => HttpRequest = _.withHeaders(Seq(
    RawHeader(API_APP_ID_HEADER, "testUserId"),
    RawHeader(API_APP_KEY_HEADER, "testUserKey")))

  val mockApiUserRequestRecord = mock[ApiUserRequestRecord]
  override def generateUserRequestRecord(httpRequest: HttpRequest, responseCode: StatusCode, responseMessage: String, requestTime: Instant)(implicit user: ApiUser) =
    Future(mockApiUserRequestRecord)

  "PainRoutes v1" should {
    "return IMPOSSIBLE if no present (GET /v1/?input) with unsovlable request" in {
      // note that there's no need for the host part in the uri:
      val input = """{"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}"""
      val request = Get(uri = s"/v1/?input=$input")

      val result = wrapUpWithUserHeaders(request) ~> routes ~> runRoute
      paintWsActorProb.expectMsg(10.seconds, ApiUserRequest(1, input))
      paintWsActorProb.reply("IMPOSSIBLE")
      dbRegistryActorProb.expectMsg(10.seconds, CreateUserRequestRecord(mockApiUserRequestRecord))

      check {
        status should ===(StatusCodes.OK)
        entityAs[String] should ===("IMPOSSIBLE")
      }(result)
    }
  }

  "PainRoutes v2" should {
    "parse out solution from (POST /v2/solve)" in {

      val paintRequest = PaintRequest(2, Seq(PaintDemands(1, Seq(PaintDemand(1, 1))), PaintDemands(2, Seq(PaintDemand(2, 0)))))
      val request = Post(uri = "/v2/solve").withEntity(Marshal(paintRequest).to[MessageEntity].futureValue)
      val result = wrapUpWithUserHeaders(request) ~> routes ~> runRoute

      paintWsActorProb.expectMsg(5.seconds, ApiUserRequest(1, """{"colors":2,"customers":2,"demands":[[1,1,1],[1,2,0]]}"""))
      paintWsActorProb.reply("1 0")
      dbRegistryActorProb.expectMsg(5.seconds, CreateUserRequestRecord(mockApiUserRequestRecord))

      check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"solutions":[{"color":1,"type":1},{"color":2,"type":0}]}""".parseJson.prettyPrint)

      } (result)
    }

    "be able to get user request history (GET /v2/history)" in {

      val request = Get(uri = "/v2/history")
      val result = wrapUpWithUserHeaders(request) ~> routes ~> runRoute
      dbRegistryActorProb.expectMsg(5.seconds, GetUserRequestRecords(1L, 50, 0))
      dbRegistryActorProb.reply(GetUserRequestRecordsResponse(Seq.empty))

      check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"records":[]}""".parseJson.prettyPrint)
      } (result)
    }

  }

}


