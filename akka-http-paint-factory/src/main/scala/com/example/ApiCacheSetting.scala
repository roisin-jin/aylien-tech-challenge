package com.example

import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{ Cache, CachingSettings }
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives.AuthenticationResult
import akka.http.scaladsl.server.RouteResult
import com.example.db.ApiUser

import scala.concurrent.duration._

object ApiCacheSetting {

  def generateUserCache(defaultCachingSettings: CachingSettings): Cache[ApiCredential, AuthenticationResult[ApiUser]] = {
    val lfuCacheSettings = defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(5)
      .withMaxCapacity(100)
      .withTimeToLive(20.minutes)
      .withTimeToIdle(10.minutes)

    val cachingSettings = defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

    LfuCache(cachingSettings)
  }

  def generatePathCache(defaultCachingSettings: CachingSettings): Cache[Uri, RouteResult] = {

    val lfuCacheSettings = defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(25)
      .withMaxCapacity(50)
      .withTimeToLive(20.seconds)
      .withTimeToIdle(10.seconds)

    val cachingSettings = defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

    LfuCache(cachingSettings)
  }
}
