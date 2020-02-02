package com.example.db

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.{ FunSuite, Matchers }
import slick.jdbc.JdbcProfile

class ApiUserDaoTest extends FunSuite with Matchers with ScalaFutures with JdbcProfile {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val testInstance = new ApiUserDao(TestDbConfig)
  val testUser = ApiUser(0, "testAppId", "testAppKey", "test@email.com", true, false)

  test("testInsertApiUser") {

    whenReady(testInstance.insertApiUser(testUser)) { inserted =>
      inserted should ===(1)
    }
  }

  test("testFindAllActiveUsers") {
    whenReady(testInstance.findAllUsers) { allUsers =>
      allUsers.isEmpty should ===(true)
    }
  }

  test("testFindByAppIdAndKey") {
    whenReady(testInstance.findByAppIdAndKey(testUser.appId, testUser.appKey)) { user =>
      user should ===(Some(testUser))
    }
  }

}
