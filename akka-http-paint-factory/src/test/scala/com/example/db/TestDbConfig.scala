package com.example.db

import akka.actor.ActorSystem
import com.example.service.DbRegistryActor
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait TestDbConfig extends SlickDbConfig {

  override val profile: JdbcProfile =  slick.jdbc.H2Profile

}

class TestDbRegistryActor(implicit system: ActorSystem) extends DbRegistryActor with TestDbConfig {

  import system.dispatcher
  import profile.api._

  val apiUserDao = new ApiUserDao()
  val apiUserRequestRecordDao = new ApiUserRequestRecordDao()

  def apply(implicit system: ActorSystem): TestDbRegistryActor = {
    Await.result(db.run(apiUserDao.table.delete), 5.seconds)
    this
  }

}