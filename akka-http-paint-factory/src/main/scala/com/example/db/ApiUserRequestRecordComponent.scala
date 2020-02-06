package com.example.db

import java.sql.Timestamp

import scala.concurrent.{ExecutionContext, Future}

case class ApiUserRequestRecord(id: Long, userId: Long, httpUri: String, httpMethod: String, postBody: Option[String],
                                responseCode: String, responseMessage: String, createdAt: Timestamp) extends Entity

trait ApiUserRequestRecordComponent extends BaseComponent with ApiUserTableDefinition { self: SlickDbConfig =>

  import profile.api._

  class ApiUserRequestRecordTable(tag: Tag) extends BaseTable[ApiUserRequestRecord](tag, "api_user_request_record") {

    val userId = column[Long]("api_user_id")
    val httpMethod = column[String]("http_method", O.Length(10))
    val httpUri = column[String]("http_uri", O.Length(1024))
    val postBody = column[Option[String]]("post_body", O.Length(2048))
    val responseCode = column[String]("response_code", O.Length(10))
    val responseMessage = column[String]("response_message", O.Length(1024))

    val apiUsersKey =
      foreignKey("API_USER_ID_FK", userId, apiUsersTable)(_.id, onDelete = ForeignKeyAction.Cascade)

    //Add id to *
    def * = (id, userId, httpMethod, httpUri, postBody, responseCode, responseMessage, createdAt) <> (ApiUserRequestRecord.tupled, ApiUserRequestRecord.unapply)

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

