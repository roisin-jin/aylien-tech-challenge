package com.example

import akka.actor.{ Actor, ActorLogging }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri }
import akka.util.ByteString

import scala.concurrent.Future

class PaintWsActor()(implicit val system: ActorSystem[_]) {

  case class Crash(replyTo: ActorRef[WsReply])
  case class WsRequest(input: String, replyTo: ActorRef[WsReply])
  case class WsReply(result: String)

  private lazy val http = Http(system)
  private val PY_APP_URL = system.settings.config.getString("paint-ws.url")
  private val INDEX_PATH = system.settings.config.getString("paint-ws.endpoints.index")

  /*
  def getInstance: Behavior[Future[WsReply]] = Behaviors.receiveMessage {
    case WsRequest(input, replyTo) =>
      replyTo ! makeWsRequest(input)
      Behaviors.same
    case Crash(replyTo) =>
      replyTo ! WsReply(s"Python app just crashed")
      Behaviors.same
  }*/

  def makeWsRequest(input: String): WsReply = {
    val uri = Uri(PY_APP_URL + Uri./ + INDEX_PATH).withQuery(Query("input" -> input))
    //http.singleRequest(HttpRequest(HttpMethods.GET, uri)) onComplete {
    WsReply("")
  }

}

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString

class Myself()(implicit val system: ActorSystem[_]) extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  val http = Http(system)

  override def preStart() = {
    http.singleRequest(HttpRequest(uri = "http://akka.io"))
      .pipeTo(self)
  }

  def receive = {
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        log.info("Got response, body: " + body.utf8String)
      }
    case resp @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }

}