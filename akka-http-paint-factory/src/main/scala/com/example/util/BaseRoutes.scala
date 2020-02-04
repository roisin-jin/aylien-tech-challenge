package com.example.util

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives.{pathEnd, post}
import akka.util.Timeout

import scala.concurrent.ExecutionContext

trait BaseRoutes {

  implicit val system: ActorSystem
  implicit def executionContext: ExecutionContext = system.dispatcher
  protected implicit lazy val timeout = Timeout.create(system.settings.config.getDuration("main-app.routes.ask-timeout"))

  val dbRegistryActor: ActorRef
  val paintWsActor: ActorRef

  protected val postSession = pathEnd & post
}
