package com.example.db

import java.time.{ Instant, ZoneId }
import slick.jdbc.MySQLProfile.api._

final case class ApiUser(id: Option[Long], email: String, company: String, hasExpired: Boolean, hasV1Access: Boolean)

class ApiUserTable(tag: Tag) extends Table[ApiUser](tag, "api_user") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def company = column[String]("company")
  def appId = column[String]("app_id")
  def appKey = column[String]("app_key")
  def hasExpired = column[Boolean]("has_expired")
  def hasV1Access = column[Boolean]("has_v1_access")

  //Add id to *
  def * = (id.?, email, company, hasExpired, hasV1Access) <> ( ApiUser.tupled, ApiUser.unapply)
}
