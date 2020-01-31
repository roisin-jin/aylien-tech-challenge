package com.example.db

import com.typesafe.config.ConfigFactory

object TestDbConfig extends DatabaseConfig {

  override val config = ConfigFactory.load("dev")
  override val driver =  slick.jdbc.H2Profile

}
