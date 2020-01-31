package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ Actor, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, StatusCodes, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.example.PaintWsActor.Crash
import com.example.db.ApiUserRequestRecord

import scala.util.Success

object PaintWsActor {

  case class Crash()
  def props: Props = Props[PaintWsActor]
}



class PaintWsActor(system: ActorSystem[_]) extends Actor {

  import system.executionContext
  implicit val classicSystem: akka.actor.ActorSystem = system.toClassic

  private val http = Http(classicSystem)
  private val PY_APP_URL = system.settings.config.getString("paint-ws.url")
  private val INDEX_PATH = system.settings.config.getString("paint-ws.endpoints.index")
  private val CRASH_PATH = system.settings.config.getString("paint-ws.endpoints.crash")


  def receive = {
    case ApiUserRequestRecord(_, userId, input, _) =>
      val requestSender = sender()
      val uri = Uri(PY_APP_URL + Uri./ + INDEX_PATH).withQuery(Query("input" -> input))
      http.singleRequest(HttpRequest(HttpMethods.GET, uri)) onComplete {
        case Success(resp) if resp.status == StatusCodes.OK =>
          Unmarshal(resp.entity).to[String] recover { case e => requestSender ! e} map (msg => sender() ! msg)
        case _ =>
          system.log.info("Paint request {} from user {} has failed", input, userId)
      }
    case Crash() =>
      val uri = Uri(PY_APP_URL + Uri./ + CRASH_PATH)
      http.singleRequest(HttpRequest(HttpMethods.GET, uri)) onComplete {
        case Success("") => sender() ! "The app has crashed"
        case _ => system.log.info("Crash request has failed")
      }
  }

}