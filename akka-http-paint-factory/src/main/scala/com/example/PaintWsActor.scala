package com.example

import akka.actor.ActorSystem
import akka.actor.{ Actor, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, StatusCodes, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.example.PaintWsActor.Crash
import com.example.db.ApiUserRequestRecord

import scala.util.Success

object PaintWsActor {

  case object Crash
  def props: Props = Props[PaintWsActor]
}



class PaintWsActor(implicit system: ActorSystem) extends Actor {

  import system.dispatcher

  private val http = Http(system)
  private val PY_APP_URL = system.settings.config.getString("paint-ws.url")
  private val INDEX_PATH = system.settings.config.getString("paint-ws.endpoints.index")
  private val CRASH_PATH = system.settings.config.getString("paint-ws.endpoints.crash")


  def receive: Receive = {
    case ApiUserRequestRecord(_, userId, input, _) =>
      val requestSender = sender()
      val uri = Uri(PY_APP_URL + Uri./ + INDEX_PATH).withQuery(Query("input" -> input))
      http.singleRequest(HttpRequest(HttpMethods.GET, uri)) onComplete {
        case Success(resp) if resp.status == StatusCodes.OK =>
          Unmarshal(resp.entity).to[String] map (msg => requestSender ! msg) recover { case e => system.log.error(e.getMessage)}
        case _ =>
          system.log.error("Paint request {} from user {} has failed", input, userId)
      }
    case Crash =>
      val uri = Uri(PY_APP_URL + Uri./ + CRASH_PATH)
      http.singleRequest(HttpRequest(HttpMethods.GET, uri)) onComplete {
        case Success(resp) if resp.status == StatusCodes.OK => system.log.info("Python app has crashed")
        case _ => system.log.error("Crash request has failed")
      }
  }

}