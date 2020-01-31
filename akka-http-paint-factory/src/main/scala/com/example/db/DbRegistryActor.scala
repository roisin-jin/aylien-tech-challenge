package com.example.db

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import com.example.ApiCredential

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

object DbRegistryActor {

  sealed trait Command

  final case class GetUser(appCreds: ApiCredential, replyTo: ActorRef[Option[ApiUser]]) extends Command

  final case class GetUserRequestRecords(userId: Long, size: Int, offset: Int, replyTo: ActorRef[Seq[ApiUserRequestRecord]]) extends Command

  final case class CreateUserRequestRecord(apiUserRequestRecord: ApiUserRequestRecord, replyTo: ActorRef[Option[Long]]) extends Command

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
        handleFutureResult(apiUserDao.findByAppIdAndKey(appCreds.appId, appCreds.appKey), replyTo, failure =>
          system.log.error("Api User with id - {} and key - {} lookup is failed: {}", appCreds.appId, appCreds.appKey, failure))
        Behaviors.same
      //Get user request history
      case GetUserRequestRecords(userId, size, offset, replyTo) =>
        handleFutureResult(apiUserRequestRecordDao.findUserRequestsHistory(userId, size, offset), replyTo, failure =>
            system.log.error("Failed to find request history for user {} : {}", userId, failure))
        Behaviors.same
      case CreateUserRequestRecord(apiUserRequestRecord, replyTo) =>
        handleFutureResult(apiUserRequestRecordDao.insertUserRequest(apiUserRequestRecord), replyTo, failure =>
          system.log.error("Cannot add request record for user id {} - {}", apiUserRequestRecord.userId, failure))
        Behaviors.same
    }
  }

  def handleFutureResult[T](futureResult: Future[T], replyTo: ActorRef[T],
    onFailure: Failure[Throwable] => Unit)(implicit ec: ExecutionContext) = futureResult onComplete {
    case Success(result) => replyTo ! result
    case Failure(failure) => onFailure(failure)
  }
}