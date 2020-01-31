package com.example.db

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import com.example.ApiCredential

import scala.util.{ Failure, Success }

object DbRegistryActor {

  sealed trait Command

  final case class GetUser(appCreds: ApiCredential, replyTo: ActorRef[Option[ApiUser]]) extends Command

  final case class GetUserRequestRecords(userId: Long, size: Int, offset: Int, replyTo: ActorRef[Seq[ApiUserRequestRecord]]) extends Command

  final case class CreateUser(apiUser: ApiUser, replyTo: ActorRef[String]) extends Command

  final case class CreateUserRequestRecord(apiUserRequestRecord: ApiUserRequestRecord, replyTo: ActorRef[String]) extends Command

}

class DbRegistryActor(val dbConfig: DatabaseConfig)(implicit system: ActorSystem[_]) {

  import DbRegistryActor._

  val apiUserDao = new ApiUserDao(dbConfig)
  val apiUserRequestRecordDao = new ApiUserRequestRecordDao(dbConfig)

  //Registering the Actor
  def register()(implicit system: ActorSystem[_]): Behavior[Command] = {
    implicit val executionContext = system.executionContext

    Behaviors.receiveMessage {
      case GetUser(appCreds, replyTo) =>
        apiUserDao.findByAppIdAndKey(appCreds.appId, appCreds.appKey) onComplete {
          case Success(result) => replyTo ! result
          case Failure(failure) =>
            system.log.error("Api User with id - {} and key - {} lookup is failed: {}", appCreds.appId, appCreds.appKey, failure)
            replyTo ! None
        }
        Behaviors.same
      //Get user request history
      case GetUserRequestRecords(userId, size, offset, replyTo) =>
        apiUserRequestRecordDao.findUserRequestsHistory(userId, size, offset) onComplete {
          case Success(result) => replyTo ! result
          case Failure(failure) =>
            system.log.error("Failed to find request history for user {} : {}", userId, failure)
            replyTo ! Seq.empty
        }
        Behaviors.same
      case CreateUser(apiUser, replyTo) =>
        apiUserDao.insertApiUser(apiUser) onComplete {
          case Success(result) => replyTo ! "SUCCESS"
          case Failure(failure) =>
            system.log.error("Failed to create new user {}", apiUser.email, failure)
            replyTo ! failure.getMessage
        }
        Behaviors.same
      case CreateUserRequestRecord(apiUserRequestRecord, replyTo) =>
        apiUserRequestRecordDao.insertUserRequest(apiUserRequestRecord) onComplete {
          case Success(result) => replyTo ! "SUCCESS"
          case Failure(failure) =>
            system.log.error("Cannot add request record for user id {} - {}", apiUserRequestRecord.userId, failure)
            replyTo ! failure.getMessage
        }
        Behaviors.same
    }
  }
}