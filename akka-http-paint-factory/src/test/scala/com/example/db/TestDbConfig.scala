package com.example.db

import akka.actor.ActorSystem
import com.example.service.DbRegistryActor
import slick.jdbc.JdbcProfile

trait TestDbConfig extends DatabaseConfig {

  override val profile: JdbcProfile =  slick.jdbc.H2Profile

}

class TestDbRegistryActor(implicit system: ActorSystem) extends DbRegistryActor with TestDbConfig {

  import system.dispatcher

  val apiUserDao = new ApiUserDao()
  val apiUserRequestRecordDao = new ApiUserRequestRecordDao()

}