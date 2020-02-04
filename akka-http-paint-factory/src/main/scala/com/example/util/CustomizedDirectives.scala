package com.example.util

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives.{complete, extract, extractClientIP, onSuccess, pass, provide, reject}
import akka.http.scaladsl.server.directives.{AuthenticationDirective, SecurityDirectives}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive0, Directive1}
import com.google.common.util.concurrent.RateLimiter

import scala.concurrent.Future

case class ApiCredential(appId: String, appKey: String)

object Limiters {
  private var rateLimiter: Option[RateLimiter] = None
  def getInstance(tps: Double): RateLimiter = rateLimiter match {
    case Some(rateLimiter) ⇒ rateLimiter
    case None ⇒
      rateLimiter = Some(RateLimiter.create(tps))
      rateLimiter.get
  }
}

object CustomizedDirectives extends SecurityDirectives {

  val API_APP_ID_HEADER = "X-PAINT-APP-ID"
  val API_APP_KEY_HEADER = "X-PAINT-APP-KEY"

  def throttle(tps: Double): Directive0 =
    extractClientIP flatMap { ip ⇒
      val rateLimiter = Limiters.getInstance(tps)
      if (rateLimiter.tryAcquire(1, 10, TimeUnit.SECONDS)) {
        pass
      } else {
        complete(StatusCodes.TooManyRequests)
      }
    }

  def extractApiCredentials: Directive1[Option[ApiCredential]] = extract[Option[ApiCredential]] { rc =>
    val appId = rc.request.headers.find(_.is(API_APP_ID_HEADER.toLowerCase))
    val appKey = rc.request.headers.find(_.is(API_APP_KEY_HEADER.toLowerCase))
    (appId, appKey) match {
      case (Some(a), Some(b)) if a.value.nonEmpty && b.value.nonEmpty => Some(ApiCredential(a.value(), b.value()))
      case _ => None
    }
  }

  def apiAuthenticateOrRejectWithChallenge[T](authenticator: Option[ApiCredential] => Future[AuthenticationResult[T]]): AuthenticationDirective[T] =
    extractApiCredentials flatMap (cred => onSuccess(authenticator(cred)) flatMap {
      case Right(user) => provide(user)
      case Left(challenge) =>
        val cause = if (cred.isEmpty) CredentialsMissing else CredentialsRejected
        reject(AuthenticationFailedRejection(cause, challenge)): Directive1[T]
    })

}
