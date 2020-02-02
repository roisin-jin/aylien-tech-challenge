package com.example.db

import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

import scala.concurrent.Future

trait BaseDao {

  val dbConfig: DatabaseConfig

  protected implicit def executeFromDb[A](action: SqlAction[A, NoStream, _ <: slick.dbio.Effect]): Future[A] = dbConfig.db.run(action)

}

class ApiUserDao(val dbConfig: DatabaseConfig) extends BaseDao {

  val table = TableQuery[ApiUserTable]

  def insertApiUser(apiUser: ApiUser): Future[Long] = table.returning(table.map(_.id)) += apiUser

  def findAllUsers: Future[Seq[ApiUser]] = table.sortBy(_.id.desc).result

  def findByAppIdAndKey(appId: String, appKey: String): Future[Option[ApiUser]] =
    table.filter(r => r.appId === appId && r.appKey === appKey).result.headOption
}

class ApiUserRequestRecordDao(val dbConfig: DatabaseConfig) extends BaseDao {

  val table = TableQuery[ApiUserRequestRecordTable]

  def insertUserRequest(apiUserRequestsHistory: ApiUserRequestRecord): Future[Long] = {
    table.returning(table.map(_.id)) += apiUserRequestsHistory
  }

  // Order by requested time descending
  def findUserRequestsHistory(userId: Long, size: Int, offset: Int): Future[Seq[ApiUserRequestRecord]] = {
    table.filter(_.userId === userId).sortBy(_.requestedTime.desc).drop(offset).take(size).result
  }

}

