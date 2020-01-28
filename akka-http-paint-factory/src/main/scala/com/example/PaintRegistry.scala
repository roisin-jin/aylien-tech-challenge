package com.example

import akka.actor.{ Actor, ActorLogging }
import akka.actor.typed.ActorSystem
import akka.http.javadsl.model.HttpResponse
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString

class PaintRegistry extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  // Get config of the python app
  private val PY_APP_URL = context.system.settings.config.getString("paint-py.api.url")

  val http = Http(context.system)

  override def preStart() = {
    http.singleRequest(HttpRequest(uri = PY_APP_URL))
      .pipeTo(self)
  }

  def receive = {
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        log.info("Got response, body: " + body.utf8String)
      }
    case resp@HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }

}
