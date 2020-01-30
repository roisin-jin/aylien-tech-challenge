package com.example.db

import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

import scala.concurrent.Future

trait BaseDao extends DatabaseConfig {

  protected implicit def executeFromDb[A](action: SqlAction[A, NoStream, _ <: slick.dbio.Effect]): Future[A] = db.run(action)

}

object UsersDao extends BaseDao {

  val apiUserTable = TableQuery[ApiUserTable]

  def findAllActiveUsers: Future[Seq[ApiUser]] = apiUserTable.filter(_.isEnabled).result

  def findByAppIdAndKey(appId: String, appKey: String): Future[ApiUser] =
    apiUserTable.filter(r => r.appId === appId && r.appKey === appKey).result.head
}

