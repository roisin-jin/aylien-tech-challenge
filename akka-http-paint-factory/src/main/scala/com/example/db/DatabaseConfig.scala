package com.example.db

import com.typesafe.config.ConfigFactory

trait DatabaseConfig {

  //set's up ConfigFactory to read from application.conf
  private val config = ConfigFactory.load()

  //Get configurations key vales for database
  private val databaseConfig = config.getConfig("database")

  val databaseUrl = databaseConfig.getString("url")
  val databaseUser = databaseConfig.getString("user")
  val databasePassword = databaseConfig.getString("password")

  val driver = slick.jdbc.MySQLProfile

  import driver.api._

  lazy val db = Database.forConfig("database")
  implicit val session: Session = db.createSession()

}

object ProdDdConfig extends DatabaseConfig {}
