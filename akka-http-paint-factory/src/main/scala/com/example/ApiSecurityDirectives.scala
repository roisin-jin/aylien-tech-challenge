package com.example

import akka.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }
import akka.http.scaladsl.server.Directives.{ extract, onSuccess, provide, reject }
import akka.http.scaladsl.server.directives.{ AuthenticationDirective, SecurityDirectives }
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, Directive1 }

import scala.concurrent.Future

case class ApiCredential(appId: String, appKey: String)

object ApiSecurityDirectives extends SecurityDirectives {

  val API_APP_ID_HEADER = "X-PAINT-APP-ID"
  val API_APP_KEY_HEADER = "X-PAINT-APP-KEY"

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
