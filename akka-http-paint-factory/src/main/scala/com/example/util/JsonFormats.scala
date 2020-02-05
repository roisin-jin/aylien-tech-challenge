package com.example.util

import java.sql.Timestamp
import java.time.Instant

import com.example.service.DbRegistryActor.GetUserResponse
import com.example.db.{ApiUser, ApiUserRequestRecord}
import spray.json.{DefaultJsonProtocol, JsNumber, JsValue, JsonFormat, PrettyPrinter}

object JsonFormats extends DefaultJsonProtocol  {

  implicit val printer = PrettyPrinter

  // Json support for timestamp format (UTC zone)
  implicit val timestampFormat: JsonFormat[Timestamp] = new JsonFormat[Timestamp] {
    override def write(obj: Timestamp): JsValue = JsNumber(obj.getTime)

    override def read(json: JsValue): Timestamp = json match {
      case JsNumber(x) => Timestamp.from(Instant.ofEpochMilli(x.toLong))
      case _ =>
        throw new IllegalArgumentException(
          s"Can not parse json value [$json] to a timestamp object")
    }
  }

  implicit val apiUserJsonFormat = jsonFormat7(ApiUser)
  implicit val apiUserRequestRecordJsonFormat = jsonFormat4(ApiUserRequestRecord)
  implicit val getUsersResponseJsonFormat = jsonFormat1(GetUserResponse)

  implicit val paintDemandJsonFormat = jsonFormat2(PaintDemand)
  implicit val paintDemandsJsonFormat = jsonFormat2(PaintDemands)
  implicit val paintRequestJsonFormat = jsonFormat2(PaintRequest)
  implicit val internalRequestJsonFormat = jsonFormat3(InternalRequest)

  implicit val paintResponseJsonFormat = jsonFormat1(PaintResponse)
}

