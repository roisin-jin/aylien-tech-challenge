package com.example.db

import java.sql.Timestamp
import java.time.Instant

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FlatSpecLike, Matchers }

import scala.concurrent.duration._
import scala.language.postfixOps

class ApiUserDaoTest extends TestKit(ActorSystem("test-system"))
    with FlatSpecLike with Matchers with ScalaFutures with ApiUserComponent with TestDbConfig {

  import system.dispatcher

  val testInstance = new ApiUserDao()
  val testUser = ApiUser(0, "testAppId_123", "testAppKey_123", "test@email.com", true, false, Timestamp.from(Instant.now()))

  require(db != null, "DB required!")

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10 seconds)

  it should "insert User in memory database" in {
    whenReady(testInstance.insertApiUser(testUser)) { inserted =>
      inserted.toInt should be > 0
    }
  }

  it should "retrieve a user by app id and app key" in {
    whenReady(testInstance.findByAppIdAndKey(testUser.appId, testUser.appKey)) { user =>
      user.isDefined should ===(true)
      user should ===(Some(testUser.copy(id = user.get.id)))
    }
  }

  it should "find all users" in {
    whenReady(testInstance.findAllUsers) { allUsers =>
      allUsers.nonEmpty should ===(true)
    }
  }
}