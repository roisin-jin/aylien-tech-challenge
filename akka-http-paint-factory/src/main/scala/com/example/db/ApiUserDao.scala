package com.example.db

import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

import scala.concurrent.Future
import ApiTable._
import com.typesafe.scalalogging.LazyLogging

trait BaseDao extends LazyLogging {

  val dbConfig: DatabaseConfig

  protected implicit def executeFromDb[A](action: SqlAction[A, NoStream, _ <: slick.dbio.Effect]): Future[A] = {
    dbConfig.db.run(action)
  }

}

object ApiTable {
  val apiUsertable = TableQuery[ApiUserTable]
  val apiUserRequestRecordtable = TableQuery[ApiUserRequestRecordTable]
}

class ApiUserDao(val dbConfig: DatabaseConfig) extends BaseDao {

  def insertApiUser(apiUser: ApiUser): Future[Long] = {
    logger.info("Create api user {}", apiUser.email)
    apiUsertable.returning(apiUsertable.map(_.id)) += apiUser
  }

  def findAllUsers: Future[Seq[ApiUser]] = apiUsertable.sortBy(_.id.desc).result

  def findByAppIdAndKey(appId: String, appKey: String): Future[Option[ApiUser]] =
    apiUsertable.filter(r => r.appId === appId && r.appKey === appKey).result.headOption
}

class ApiUserRequestRecordDao(val dbConfig: DatabaseConfig) extends BaseDao {

  def insertUserRequest(apiUserRequestsHistory: ApiUserRequestRecord): Future[Long] = {
    apiUserRequestRecordtable.returning(apiUserRequestRecordtable.map(_.id)) += apiUserRequestsHistory
  }

  // Order by requested time descending
  def findUserRequestsHistory(userId: Long, size: Int, offset: Int): Future[Seq[ApiUserRequestRecord]] = {
    apiUserRequestRecordtable.filter(_.userId === userId).sortBy(_.requestedTime.desc).drop(offset).take(size).result
  }

}

