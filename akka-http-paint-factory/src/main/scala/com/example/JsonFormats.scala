package com.example

import com.example.db.DbRegistryActor.{ GetUserRequestRecordsResponse, GetUserResponse }
import com.example.db.{ ApiUser, ApiUserRequestRecord }
import spray.json.DefaultJsonProtocol

object JsonFormats extends DefaultJsonProtocol  {
  // import the default encoders for primitive types (Int, String, Lists etc)

  implicit val apiUserJsonFormat = jsonFormat6(ApiUser)
  implicit val apiUserRequestRecordJsonFormat = jsonFormat4(ApiUserRequestRecord)
  implicit val getUsersResponseJsonFormat = jsonFormat2(GetUserResponse)

  implicit val paintDemandJsonFormat = jsonFormat2(PaintDemand)
  implicit val paintDemandsJsonFormat = jsonFormat2(PaintDemands)
  implicit val paintRequestJsonFormat = jsonFormat2(PaintRequest)
  implicit val internalRequestJsonFormat = jsonFormat3(InternalRequest)
}

