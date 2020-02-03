package com.example.db

import java.sql.Timestamp

import scala.concurrent.{ ExecutionContext, Future }

case class ApiUser(id: Long, appId: String, appKey: String, email: String,
  hasExpired: Boolean, hasV1Access: Boolean, createdAt: Timestamp) extends Entity {
  def hasValidAccess: Boolean = !hasExpired
}

trait ApiUserTableDefinition extends TableDefinition { self: DatabaseConfig =>

  import profile.api._

  class ApiUserTable(tag: Tag) extends BaseTable[ApiUser](tag, "api_user") {

    val appId = column[String]("app_id", O.Length(64, varying = true))
    val appKey = column[String]("app_key", O.Length(64, varying = true))
    val email = column[String]("email", O.Length(128, varying = true))
    val hasExpired = column[Boolean]("has_expired")
    val hasV1Access = column[Boolean]("has_v1_access")
    val idx = index("app_creds_UNIQUE", (appId, appKey), unique = true)

    //Add id to *
    def * = (id, appId, appKey, email, hasExpired, hasV1Access, createdAt) <> (ApiUser.tupled, ApiUser.unapply)
  }

  val apiUsersTable = TableQuery[ApiUserTable]
}

trait ApiUserComponent extends BaseComponent with ApiUserTableDefinition { self: DatabaseConfig =>

  import profile.api._

  class ApiUserDao(implicit ex: ExecutionContext) extends BaseDao[ApiUser, ApiUserTable] {

    override val table = TableQuery[ApiUserTable]

    def insertApiUser(apiUser: ApiUser): Future[Long] = table.returning(table.map(_.id)) += apiUser

    def findAllUsers: Future[Seq[ApiUser]] = table.sortBy(_.id.desc).result

    def findByAppIdAndKey(appId: String, appKey: String): Future[Option[ApiUser]] =
      table.filter(r => r.appId === appId && r.appKey === appKey).result.headOption
  }

}
