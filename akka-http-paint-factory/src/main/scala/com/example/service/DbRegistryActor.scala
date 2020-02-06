package com.example.service

import akka.actor.{ Actor, ActorLogging, ActorSystem }
import com.example.db._
import com.example.util.ApiCredential

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

  case class CreateUserRequestRecord(apiUserRequestRecord: ApiUserRequestRecord)

}

trait DbRegistryActor extends Actor with ActorLogging with DatabaseConfig
  with ApiUserComponent with ApiUserRequestRecordComponent {

  import DbRegistryActor._
  import context.dispatcher

  val apiUserDao: ApiUserDao
  val apiUserRequestRecordDao: ApiUserRequestRecordDao

  //Registering the Actor
  def receive: Receive = {
    case GetUser(appCreds) =>
      val replyTo = sender()
      apiUserDao.findByAppIdAndKey(appCreds.appId, appCreds.appKey) onComplete {
        case Success(result) =>
          val users = result map (Seq(_)) getOrElse Seq.empty
          replyTo ! GetUserResponse(users)
        case Failure(failure) =>
          log.error(failure, "Api User with id {} and key {} lookup is failed", appCreds.appId, appCreds.appKey)
      }
    case GetAllUsers =>
      val replyTo = sender()
      apiUserDao.findAllUsers map (GetUserResponse(_)) onComplete {
        case Success(result) => replyTo ! result
        case Failure(failure) =>
          log.error(failure, "Failed to all users")
      }
    case GetUserRequestRecords(userId, size, offset) =>
      val replyTo = sender()
      apiUserRequestRecordDao.findUserRequestsHistory(userId, size, offset) onComplete {
        case Success(result) => replyTo ! GetUserRequestRecordsResponse(result)
        case Failure(failure) =>
          log.error(failure, "Failed to find request history for user {}", userId)
      }
    case CreateUser(apiUser) =>
      val replyTo = sender()
      apiUserDao.insertApiUser(apiUser) onComplete {
        case Success(_) => replyTo ! "SUCCESS"
        case Failure(failure) =>
          log.error(failure, "Failed to create new user {}", apiUser.email)
      }
    case CreateUserRequestRecord(apiUserRequestRecord) =>
      val replyTo = sender()
      apiUserRequestRecordDao.insertUserRequest(apiUserRequestRecord)  onComplete {
        case Success(_) => replyTo ! "SUCCESS"
        case Failure(failure) =>
          log.error(failure, "Cannot add request record for user id {}", apiUserRequestRecord.userId)
      }
  }
}