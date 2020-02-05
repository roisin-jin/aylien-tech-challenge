package com.example.routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, extractUri, pathEnd, post}
import akka.http.scaladsl.server.ExceptionHandler
import akka.util.Timeout

import scala.concurrent.ExecutionContext

trait BaseRoutes {

  implicit val system: ActorSystem

  implicit def executionContext: ExecutionContext = system.dispatcher

  // time out for akka ask pattern
  protected implicit lazy val timeout = Timeout.create(system.settings.config.getDuration("main-app.routes.ask-timeout"))

  def dbRegistryActor: ActorRef

  def paintWsActor: ActorRef

  protected val postSession = pathEnd & post

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: Exception => extractUri { uri =>
      system.log.error(e, s"Internal Server Error on ${uri} endpoint")
      complete(StatusCodes.InternalServerError)
    }
  }
}
