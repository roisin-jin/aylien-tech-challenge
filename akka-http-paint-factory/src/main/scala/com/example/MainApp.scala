package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.example.db.{ DbRegistryActor, ProdDdConfig }
import slick.basic.DatabaseConfig

import scala.util.{ Failure, Success }

object MainApp {

  private def startHttpServer(routes: Route, system: ActorSystem[_]): Unit = {
    // Akka HTTP needs a classic ActorSystem to start
    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
    import system.executionContext

    Http().bindAndHandle(routes, "localhost", 9000) onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      implicit val system = context.system

      val dbRegistryActor = context.spawn(new DbRegistryActor(ProdDdConfig).register(), "DbRegistryActor")
      context.watch(dbRegistryActor)

      val routes = new PaintRoutes(dbRegistryActor)
      startHttpServer(routes.routes, context.system)

      Behaviors.empty
    }

    ActorSystem[Nothing](rootBehavior, "PaintApiServer-V2")
  }
}

