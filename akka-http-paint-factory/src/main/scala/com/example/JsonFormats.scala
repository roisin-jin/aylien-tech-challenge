package com.example

import com.example.db.ApiUser
import spray.json.DefaultJsonProtocol

object JsonFormats extends DefaultJsonProtocol  {
  // import the default encoders for primitive types (Int, String, Lists etc)

  implicit val apiUserJsonFormat = jsonFormat5(ApiUser)
  implicit val paintDemandJsonFormat = jsonFormat2(PaintDemand)
  implicit val paintDemandsJsonFormat = jsonFormat2(PaintDemands)
  implicit val paintRequestJsonFormat = jsonFormat2(PaintRequest)
  implicit val internalRequestJsonFormat = jsonFormat3(InternalRequest)
}

