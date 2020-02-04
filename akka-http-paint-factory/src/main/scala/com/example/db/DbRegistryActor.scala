package com.example.db

import akka.actor.{Actor, ActorLogging}
import com.example.util.ApiCredential

import scala.util.{Failure, Success}

object ProdDbRegistryActor extends DbRegistryActor with ProdDdConfig {}

object DbRegistryActor {

  final case class GetUser(appCreds: ApiCredential)
  final case object GetAllUsers
  final case class GetUserResponse(users: Seq[ApiUser], message: String)

  final case class GetUserRequestRecords(userId: Long, size: Int, offset: Int)
  final case class GetUserRequestRecordsResponse(records: Seq[ApiUserRequestRecord])

  final case class CreateUser(apiUser: ApiUser)

  final case class CreateUserRequestRecord(apiUserRequestRecord: ApiUserRequestRecord)
}

trait DbRegistryActor extends Actor with ActorLogging with DatabaseConfig
  with ApiUserComponent with ApiUserRequestRecordComponent {

  import DbRegistryActor._
  import context.dispatcher

  val apiUserDao = new ApiUserDao()
  val apiUserRequestRecordDao = new ApiUserRequestRecordDao()

  //Registering the Actor
  def receive: Receive = {
    case GetUser(appCreds) =>
      apiUserDao.findByAppIdAndKey(appCreds.appId, appCreds.appKey) onComplete {
        case Success(result) =>
          val users = result map (Seq(_)) getOrElse Seq.empty
          sender ! GetUserResponse(users, "SUCCESS")
        case Failure(failure) =>
          log.error("Api User with id - {} and key - {} lookup is failed: {}", appCreds.appId, appCreds.appKey, failure)
          sender ! GetUserResponse(Seq.empty, failure.getMessage)
      }
    case GetAllUsers =>
      apiUserDao.findAllUsers onComplete {
        case Success(users) => sender ! GetUserResponse(users, "SUCCESS")
        case Failure(failure) =>
          log.error("Cant load all users!", failure)
          sender ! GetUserResponse(Seq.empty, failure.getMessage)
      }
    //Get user request history
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
          sender ! failure.getMessage
      }
    case CreateUserRequestRecord(apiUserRequestRecord) =>
      apiUserRequestRecordDao.insertUserRequest(apiUserRequestRecord) andThen {
        case Failure(failure) =>
          log.error("Cannot add request record for user id {} - {}", apiUserRequestRecord.userId, failure)
      }
  }
}