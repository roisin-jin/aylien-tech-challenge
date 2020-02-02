package com.example

import java.util.concurrent.TimeUnit

import com.google.common.util.concurrent.RateLimiter
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.extractClientIP
import akka.http.scaladsl.model.StatusCodes

object Limiters {
  private var rateLimiter: Option[RateLimiter] = None
  def getInstance(tps: Double): RateLimiter = rateLimiter match {
    case Some(rateLimiter) ⇒ rateLimiter
    case None ⇒
      rateLimiter = Some(RateLimiter.create(tps))
      rateLimiter.get
  }
}

trait Throttle {
  def throttle(tps: Double): Directive0 =
    extractClientIP flatMap { ip ⇒
      val rateLimiter = Limiters.getInstance(tps)
      if (rateLimiter.tryAcquire(1, 10, TimeUnit.SECONDS)) {
        pass
      } else {
        complete(StatusCodes.TooManyRequests)
      }
    }
}
