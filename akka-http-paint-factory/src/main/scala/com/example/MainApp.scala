package com.example

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.example.db.DbRegistryActor

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.{ Failure, Success }

object MainApp extends App {

  implicit val system: ActorSystem = ActorSystem("PaintApiServer-V2")
  implicit val executionContext: ExecutionContext = system.dispatcher

  val dbRegistryActor: ActorRef = system.actorOf(DbRegistryActor.props,"DbRegistryActor")
  val paintWsActor: ActorRef = system.actorOf(PaintWsActor.props, "paintWsActor")

  val routes: Route = new PaintRoutes(dbRegistryActor, paintWsActor).routes

  Http().bindAndHandle(routes, "localhost", 9000) onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
    case Failure(ex) =>
      system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}

