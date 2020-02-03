package com.example.db

import slick.jdbc.JdbcProfile

trait TestDbConfig extends DatabaseConfig {

  override val profile: JdbcProfile =  slick.jdbc.H2Profile

}