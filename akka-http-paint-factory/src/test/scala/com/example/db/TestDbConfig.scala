package com.example.db

import com.typesafe.config.ConfigFactory

trait TestDbConfig extends DatabaseConfig {

  override val config = ConfigFactory.load("dev")
  override val profile =  slick.jdbc.H2Profile

}
