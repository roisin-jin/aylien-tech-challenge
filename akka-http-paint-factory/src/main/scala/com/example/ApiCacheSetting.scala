package com.example

import akka.actor.ActorSystem
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{ Cache, CachingSettings }
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.RouteResult
import com.example.db.ApiUser

import scala.concurrent.duration._

object ApiCacheSetting {

  def generateAccountCache(implicit system: ActorSystem): Cache[ApiCredential, ApiUser] = {

    val defaultCachingSettings = CachingSettings(system)
    val lfuCacheSettings = defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(25)
      .withMaxCapacity(100)
      .withTimeToLive(10 minutes)
      .withTimeToIdle(5 minutes)

    val cachingSettings = defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

    LfuCache(cachingSettings)
  }

  def generateOperationCache(implicit system: ActorSystem): Cache[Uri, RouteResult] = {

    val defaultCachingSettings = CachingSettings(system)

    val lfuCacheSettings =
      defaultCachingSettings.lfuCacheSettings
        .withInitialCapacity(25)
        .withMaxCapacity(50)
        .withTimeToLive(20.seconds)
        .withTimeToIdle(10.seconds)

    val cachingSettings =
      defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

    LfuCache(cachingSettings)
  }
}
