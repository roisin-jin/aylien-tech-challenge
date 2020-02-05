package com.example.service

import akka.actor.{ Actor, ActorLogging, ActorSystem }
import com.example.db._
import com.example.util.ApiCredential

import scala.concurrent.Future
import scala.util.{ Failure, Success }

class ProdDbRegistryActor(implicit system: ActorSystem) extends DbRegistryActor with ProdDdConfig {
  import system.dispatcher
  val apiUserDao: ApiUserDao = new ApiUserDao()
  val apiUserRequestRecordDao: ApiUserRequestRecordDao = new ApiUserRequestRecordDao()
}

object DbRegistryActor {

  final case class GetUser(appCreds: ApiCredential)
  final case object GetAllUsers
  final case class GetUserResponse(users: Seq[ApiUser])

  final case class GetUserRequestRecords(userId: Long, size: Int, offset: Int)
  final case class GetUserRequestRecordsResponse(records: Seq[ApiUserRequestRecord])

  final case class CreateUser(apiUser: ApiUser)

  final case class CreateUserRequestRecord(apiUserRequestRecord: ApiUserRequestRecord)
}

trait DbRegistryActor extends Actor with ActorLogging with DatabaseConfig
  with ApiUserComponent with ApiUserRequestRecordComponent {

  import DbRegistryActor._
  import context.dispatcher

  def apiUserDao: ApiUserDao
  def apiUserRequestRecordDao: ApiUserRequestRecordDao

  //Registering the Actor
  def receive: Receive = {
    case GetUser(appCreds) =>
      apiUserDao.findByAppIdAndKey(appCreds.appId, appCreds.appKey) onComplete {
        case Success(result) =>
          val users = result map (Seq(_)) getOrElse Seq.empty
          sender ! GetUserResponse(users)
        case Failure(failure) =>
          log.error("Api User with id - {} and key - {} lookup is failed: {}", appCreds.appId, appCreds.appKey, failure)
      }
    case GetAllUsers =>
      apiUserDao.findAllUsers map (GetUserResponse(_)) onComplete {
        case Success(result) => sender ! result
        case Failure(failure) =>
          log.error("Failed to all users - {}", failure)
      }
    case GetUserRequestRecords(userId, size, offset) =>
      apiUserRequestRecordDao.findUserRequestsHistory(userId, size, offset) onComplete {
        case Success(result) => sender ! GetUserRequestRecordsResponse(result)
        case Failure(failure) =>
          log.error("Failed to find request history for user {} : {}", userId, failure)
      }
    case CreateUser(apiUser) =>
      apiUserDao.insertApiUser(apiUser) onComplete {
        case Success(_) => sender ! "SUCCESS"
        case Failure(failure) =>
          log.error("Failed to create new user {}", apiUser.email, failure)
      }
    case CreateUserRequestRecord(apiUserRequestRecord) =>
      apiUserRequestRecordDao.insertUserRequest(apiUserRequestRecord) andThen {
        case Failure(failure) =>
          log.error("Cannot add request record for user id {} - {}", apiUserRequestRecord.userId, failure)
      }
  }
}