package com.example

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.caching.scaladsl.CachingSettings
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import com.example.service.{PaintWsActor, ProdDbRegistryActor}
import com.example.util.ApiCacheSetting
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

object MainApp extends App with PaintRoutes with SuperAdminRoutes {

  implicit val system: ActorSystem = ActorSystem("PaintApiServer-V2")

  val dbRegistryActor: ActorRef = system.actorOf(Props(new ProdDbRegistryActor),"DbRegistryActor")
  val paintWsActor: ActorRef = system.actorOf(Props(new PaintWsActor()), "PaintWsActor")
  val defaultCacheSettings = CachingSettings(system)
  val userCache = ApiCacheSetting.generateUserCache(defaultCacheSettings)
  val wsCache = ApiCacheSetting.generatePathCache(defaultCacheSettings)

  // Set up HTTPS support for api credentials
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

  startsApiApp(paintRoutes, 9000)
  startsApiApp(adminRoutes, 9001)

  Await.result(system.whenTerminated, Duration.Inf)

  def startsApiApp(route: Route, port: Int) = {
    Http().bindAndHandle(route, "localhost", port, connectionContext = https) onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at https://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}