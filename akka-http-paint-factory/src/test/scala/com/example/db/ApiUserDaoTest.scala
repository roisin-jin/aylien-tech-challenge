package com.example.db

import java.sql.Timestamp
import java.time.Instant

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FlatSpecLike, Matchers }

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class ApiUserDaoTest extends TestKit(ActorSystem("test-system"))
    with FlatSpecLike with Matchers with ScalaFutures with ApiUserComponent with TestDbConfig {

  implicit val ec: ExecutionContext = system.dispatcher

  val testInstance = new ApiUserDao()
  val testUser = ApiUser(0, "testAppId", "testAppKey", "test@email.com", true, false, Timestamp.from(Instant.now()))

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(5 seconds)

  "The generic repository" should "insert User in memory database" in {
    whenReady(testInstance.insertApiUser(testUser)) { inserted =>
      inserted should ===(1)
    }
  }

  it should "retrieve a user by app id and app key" in {
    whenReady(testInstance.findByAppIdAndKey(testUser.appId, testUser.appKey)) { user =>
      user should ===(Some(testUser))
    }
  }

  it should "find all users" in {
    whenReady(testInstance.findAllUsers) { allUsers =>
      allUsers.isEmpty should ===(true)
    }
  }
}