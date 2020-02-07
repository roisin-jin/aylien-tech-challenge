package com.example

import java.io.InputStream
import java.security.{ KeyStore, SecureRandom }

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.caching.scaladsl.CachingSettings
import akka.http.scaladsl.server.RouteConcatenation._
import akka.http.scaladsl.{ ConnectionContext, Http, HttpsConnectionContext }
import com.example.routes.{ PaintRoutes, SuperAdminRoutes }
import com.example.service.{ PaintWsActor, ProdDbRegistryActor }
import com.example.util.ApiCacheSetting
import javax.net.ssl.{ KeyManagerFactory, SSLContext, TrustManagerFactory }

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success }

object MainApp extends App with PaintRoutes with SuperAdminRoutes {

  implicit val system: ActorSystem = ActorSystem("PaintApiServer")

  // Setup DB actor
  val dbRegistryActor: ActorRef = system.actorOf(Props(new ProdDbRegistryActor),"DbRegistryActor")
  // Setup WS actor
  val paintWsActor: ActorRef = system.actorOf(Props(new PaintWsActor), "PaintWsActor")

  val defaultCacheSettings = CachingSettings(system)
  val userCache = ApiCacheSetting.generateUserCache(defaultCacheSettings)
  val wsCache = ApiCacheSetting.generateWsResultCache(defaultCacheSettings)

  val localHttpsSupportEnabled = system.settings.config.getBoolean("main-app.https.local-support-enabled")

  val route = paintRoutes ~ adminRoutes

  val httpServerBinding = if (localHttpsSupportEnabled) {
    // Set up local HTTPS support for api credentials if enabled
    val secret = system.settings.config.getString("main-app.https.secret").toCharArray
    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("server.pkcs12")

    require(keystore != null, "Keystore required!")
    ks.load(keystore, secret)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, secret)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)

    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)
    Http().bindAndHandle(route, "0.0.0.0", 9000, connectionContext = https)
  } else {
    Http().bindAndHandle(route, "0.0.0.0", 9000)
  }

  httpServerBinding onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      val httpConnection = if (localHttpsSupportEnabled) "https" else "http"
      system.log.info("Server online at {}://{}:{}/", httpConnection, address.getHostString, address.getPort)
    case Failure(ex) =>
      system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}