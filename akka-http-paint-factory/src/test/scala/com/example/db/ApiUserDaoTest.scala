package com.example.db

import org.h2.engine.Database
import org.scalatest.{ FunSuite, Matchers }
import org.scalatest.concurrent.ScalaFutures

object TestDbConfig extends DatabaseConfig {

  override lazy val db = Database
}

class ApiUserDaoTest extends FunSuite with Matchers with ScalaFutures {

  val testInstance = new ApiUserDao()

  test("testFindAllActiveUsers") {

  }

  test("testInsertApiUser") {

  }

  test("testFindByAppIdAndKey") {

  }

}
