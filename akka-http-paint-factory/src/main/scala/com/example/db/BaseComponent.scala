package com.example.db

import java.sql.Timestamp

import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

trait Entity {
  def id: Long
  def createdAt: Timestamp
}

/**
  * The repo definition which should be used within an entity repo
  */
trait TableDefinition { self: SlickDbConfig =>

  import profile.api._

  /**
    * The [[BaseTable]] describes the basic [[Entity]]
    */
  abstract class BaseTable[E <: Entity: ClassTag](tag: Tag, tableName: String, schemaName: Option[String] = None)
    extends Table[E](tag, schemaName, tableName) {

    val id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    val createdAt = column[Timestamp]("created_at", SqlType("timestamp not null default CURRENT_TIMESTAMP"))
  }

}

trait BaseComponent extends TableDefinition { self: SlickDbConfig =>

  import profile.api._

  abstract class BaseDao[E <: Entity, T <: BaseTable[E]](implicit ex: ExecutionContext) {

    val table: TableQuery[T]

    def insert(entity: E): Future[E] = table returning table += entity
  }

}