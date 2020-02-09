package com.example.util

import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import akka.http.scaladsl.server.Directives.AuthenticationResult
import akka.http.scaladsl.server.RouteResult
import com.example.db.ApiUser

import scala.concurrent.duration._

object ApiCacheSetting {

  def generateUserCache(defaultCachingSettings: CachingSettings): Cache[ApiCredential, AuthenticationResult[ApiUser]] = {
    val lfuCacheSettings = defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(5)
      .withMaxCapacity(100)
      .withTimeToLive(10.minutes)
      .withTimeToIdle(5.minutes)

    val cachingSettings = defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

    LfuCache(cachingSettings)
  }

  def generateWsResultCache(defaultCachingSettings: CachingSettings): Cache[String, String] = {

    val lfuCacheSettings = defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(25)
      .withMaxCapacity(50)
      .withTimeToLive(20.minutes)
      .withTimeToIdle(10.minutes)

    val cachingSettings = defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

    LfuCache(cachingSettings)
  }
}
