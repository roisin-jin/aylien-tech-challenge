package com.example.service

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.util.{Failure, Success}

object PaintWsActor {

  case class ApiUserRequest(userId: Long, input: String)
  case object Crash
  def props: Props = Props[PaintWsActor]
}

class PaintWsActor(implicit system: ActorSystem) extends Actor {

  import com.example.service.PaintWsActor._
  import system.dispatcher

  private val http = Http(system)
  private val PY_APP_URL = system.settings.config.getString("paint-ws.url")
  private val INDEX_PATH = system.settings.config.getString("paint-ws.endpoints.index")
  private val CRASH_PATH = system.settings.config.getString("paint-ws.endpoints.crash")


  def receive: Receive = {
    case ApiUserRequest(userId, input) =>
      val replyTo = sender()
      val uri = Uri(PY_APP_URL + INDEX_PATH).withQuery(Query("input" -> input))
      system.log.info("Forwarding input request {} to python ws", input)
      http.singleRequest(HttpRequest(HttpMethods.GET, uri)) onComplete {
        case Success(resp) =>
          system.log.info("Get response {} from paint ws", resp)
          Unmarshal(resp.entity).to[String] map (msg => replyTo ! msg) recover { case e =>
            system.log.error(e.getMessage, "Failed to parse response entity!")}
        case Failure(exception) =>
          system.log.error(exception,"Paint request {} from user {} has failed", input, userId)
      }
    case Crash =>
      val uri = Uri(PY_APP_URL + Uri./ + CRASH_PATH)
      http.singleRequest(HttpRequest(HttpMethods.GET, uri)) onComplete {
        case Success(resp) => system.log.info("Python app has crashed")
        case Failure(exception) => system.log.error(exception, "Crash request has failed")
      }
  }

}