package com.example.db

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FunSuite, Matchers }

class ApiUserDaoTest extends FunSuite with Matchers with ScalaFutures {

  val testInstance = new ApiUserDao(TestDbConfig)
  val testUser = ApiUser(None, "testAppId", "testappKey", "test@email.com", true, false)

  test("testInsertApiUser") {

    whenReady(testInstance.insertApiUser(testUser)) { inserted =>
      inserted should ===(1)
    }
  }

  test("testFindAllActiveUsers") {
    whenReady(testInstance.findAllActiveUsers) { allUsers =>
      allUsers.isEmpty should ===(true)
    }
  }

  test("testFindByAppIdAndKey") {
    whenReady(testInstance.findByAppIdAndKey(testUser.appId, testUser.appKey)) { user =>
      user should ===(Some(testUser))
    }
  }

}
