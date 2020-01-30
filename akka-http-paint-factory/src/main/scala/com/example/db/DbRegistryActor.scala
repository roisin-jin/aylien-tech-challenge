package com.example.db

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import com.example.ApiCredential

import scala.concurrent.Future
import scala.util.{ Failure, Success }

object DbRegistryActor {

  sealed trait Command

  final case class GetUser(appCreds: ApiCredential, replyTo: ActorRef[Option[ApiUser]]) extends Command

  final case class GetUserRequestRecords(userId: Long, replyTo: ActorRef[String]) extends Command

}

class DbRegistryActor()(implicit system: ActorSystem[_]) {

  import DbRegistryActor._
  import system.executionContext

  //Registering the Actor
  def apply(): Behavior[Command] = Behaviors.receiveMessage {
    case GetUser(appCreds, replyTo) =>
      ApiUserDao.findByAppIdAndKey(appCreds.appId, appCreds.appKey) onComplete {
        case Success(usr) => replyTo ! usr
        case Failure(failure) =>
          replyTo ! None
          system.log.error("Api User with id - {} and key - {} lookup is failed: {}", appCreds.appId, appCreds.appKey, failure)
      }
      Behaviors.same
    //Get user request history
    case GetUserRequestRecords(id, replyTo) =>
      Future("") onComplete {
        case Success(usrHistory) => replyTo ! usrHistory
        case Failure(failure) =>
          replyTo ! ""
          system.log.error("Failed to find request history for user {} : {}", id, failure)
      }
      Behaviors.same
  }
}