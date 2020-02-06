package com.example.db

import java.sql.Timestamp

import scala.concurrent.{ ExecutionContext, Future }

case class ApiUserRequestRecord(id: Long, userId: Long, requestInput: String, response: String, createdAt: Timestamp) extends Entity

trait ApiUserRequestRecordComponent extends BaseComponent with ApiUserTableDefinition { self: DatabaseConfig =>

  import profile.api._

  class ApiUserRequestRecordTable(tag: Tag) extends BaseTable[ApiUserRequestRecord](tag, "api_user_request_record") {

    val userId = column[Long]("api_user_id")

    val requestInput = column[String]("request_input", O.Length(1024))

    val response = column[String]("response", O.Length(1024))

    val apiUsersKey =
      foreignKey("API_USER_ID_FK", userId, apiUsersTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    //Add id to *
    def * = (id, userId, requestInput, response, createdAt) <> (ApiUserRequestRecord.tupled, ApiUserRequestRecord.unapply)

  }

  class ApiUserRequestRecordDao(implicit ex: ExecutionContext) extends BaseDao[ApiUserRequestRecord, ApiUserRequestRecordTable] {

    override val table = TableQuery[ApiUserRequestRecordTable]

    def insertUserRequest(apiUserRequestsHistory: ApiUserRequestRecord): Future[Long] = {
      table.returning(table.map(_.id)) += apiUserRequestsHistory
    }

    // Order by requested time descending
    def findUserRequestsHistory(userId: Long, size: Int, offset: Int): Future[Seq[ApiUserRequestRecord]] = {
      table.filter(_.userId === userId).sortBy(_.createdAt.desc).drop(offset).take(size).result
    }

  }

}

