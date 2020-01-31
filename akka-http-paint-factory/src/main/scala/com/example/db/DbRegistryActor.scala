package com.example.db

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import com.example.ApiCredential

import scala.util.{ Failure, Success }

object DbRegistryActor {

  final case class GetUser(appCreds: ApiCredential)
  final case class GetUserResponse(user: Option[ApiUser])

  final case class GetUserRequestRecords(userId: Long, size: Int, offset: Int)

  final case class CreateUser(apiUser: ApiUser)

  final case class CreateUserRequestRecord(apiUserRequestRecord: ApiUserRequestRecord)

  def props: Props = Props[DbRegistryActor]

}

class DbRegistryActor(val dbConfig: DatabaseConfig)(implicit system: ActorSystem) extends Actor with ActorLogging {

  import DbRegistryActor._
  import system.dispatcher

  val apiUserDao = new ApiUserDao(dbConfig)
  val apiUserRequestRecordDao = new ApiUserRequestRecordDao(dbConfig)

  //Registering the Actor
  def receive: Receive = {
    case GetUser(appCreds) =>
      apiUserDao.findByAppIdAndKey(appCreds.appId, appCreds.appKey) onComplete {
        case Success(result) => sender ! GetUserResponse(result)
        case Failure(failure) =>
          log.error("Api User with id - {} and key - {} lookup is failed: {}", appCreds.appId, appCreds.appKey, failure)
          sender ! GetUserResponse(None)
      }
    //Get user request history
    case GetUserRequestRecords(userId, size, offset) =>
      apiUserRequestRecordDao.findUserRequestsHistory(userId, size, offset) onComplete {
        case Success(result) => sender ! result
        case Failure(failure) =>
          log.error("Failed to find request history for user {} : {}", userId, failure)
          sender ! Seq.empty
      }
    case CreateUser(apiUser) =>
      apiUserDao.insertApiUser(apiUser) onComplete {
        case Success(_) => sender ! "SUCCESS"
        case Failure(failure) =>
          log.error("Failed to create new user {}", apiUser.email, failure)
          sender ! failure.getMessage
      }
    case CreateUserRequestRecord(apiUserRequestRecord) =>
      apiUserRequestRecordDao.insertUserRequest(apiUserRequestRecord) onComplete {
        case Success(_) => sender ! "SUCCESS"
        case Failure(failure) =>
          log.error("Cannot add request record for user id {} - {}", apiUserRequestRecord.userId, failure)
          sender ! failure.getMessage
      }
  }
}