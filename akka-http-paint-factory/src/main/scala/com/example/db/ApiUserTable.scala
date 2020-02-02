package com.example.db

import slick.jdbc.MySQLProfile.api._

case class ApiUser(id: Long, appId: String, appKey: String,
  email: String, hasExpired: Boolean, hasV1Access: Boolean) {
  def hasValidAccess: Boolean = !hasExpired
}

case class ApiUserRequestRecord(id: Long, userId: Long, requestInput: String, requestedTime: Long)

class ApiUserTable(tag: Tag) extends Table[ApiUser](tag, "api_user") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email", O.Length(128, varying = true))
  def appId = column[String]("app_id", O.Length(64, varying = true))
  def appKey = column[String]("app_key", O.Length(64, varying = true))
  def hasExpired = column[Boolean]("has_expired")
  def hasV1Access = column[Boolean]("has_v1_access")
  def idx = index("app_creds_UNIQUE", (appId, appKey), unique = true)

  //Add id to *
  def * = (id, appId, appKey, email, hasExpired, hasV1Access) <> ( ApiUser.tupled, ApiUser.unapply)
}

class ApiUserRequestRecordTable(tag: Tag) extends Table[ApiUserRequestRecord](tag, "api_user_request_record") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("api_user_id")
  def requestInput = column[String]("request_input", O.Length(256, varying = true))
  def requestedTime = column[Long]("requested_time")
  def apiUsers =
    foreignKey("API_USER_ID_FK", userId, ApiTable.apiUsertable)(_.id, onDelete=ForeignKeyAction.Cascade)

  //Add id to *
  def * = (id, userId, requestInput, requestedTime) <> ( ApiUserRequestRecord.tupled, ApiUserRequestRecord.unapply)

}
