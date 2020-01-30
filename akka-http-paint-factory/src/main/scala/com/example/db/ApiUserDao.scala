package com.example.db

import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

import scala.concurrent.Future

trait BaseDao extends DatabaseConfig {

  protected implicit def executeFromDb[A](action: SqlAction[A, NoStream, _ <: slick.dbio.Effect]): Future[A] = db.run(action)

}

object ApiUserDao extends BaseDao {

  private lazy val apiUserTable = TableQuery[ApiUserTable]

  def findAllActiveUsers: Future[Seq[ApiUser]] = apiUserTable.filterNot(_.hasExpired).result

  def findByAppIdAndKey(appId: String, appKey: String): Future[Option[ApiUser]] =
    apiUserTable.filter(r => r.appId === appId && r.appKey === appKey).result.headOption
}

