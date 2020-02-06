package com.example.db

import com.typesafe.config.{Config, ConfigFactory}
import slick.jdbc.JdbcProfile
import slick.sql.SqlAction

import scala.concurrent.Future

trait SlickDbConfig {

  val config: Config = ConfigFactory.load()

  //Get configurations key vales for database
  lazy val databaseConfig = config.getConfig("database")
  lazy val databaseUrl = databaseConfig.getString("url")
  lazy val databaseUser = databaseConfig.getString("user")
  lazy val databasePassword = databaseConfig.getString("password")

  val profile: JdbcProfile
  import profile.api._

  lazy val db = Database.forConfig("database")
  implicit lazy val session: Session = db.createSession()
  protected implicit def executeFromDb[A](action: SqlAction[A, NoStream, _ <: slick.dbio.Effect]): Future[A] = db.run(action)

}

trait ProdDdConfig extends SlickDbConfig {

  override val profile: JdbcProfile =  slick.jdbc.MySQLProfile

}