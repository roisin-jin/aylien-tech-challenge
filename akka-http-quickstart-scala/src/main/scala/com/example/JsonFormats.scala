package com.example

import com.example.UserRegistry.ActionPerformed


import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat3(User)
  implicit val usersJsonFormat = jsonFormat1(Users)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

  implicit val paintDemandJsonFormat = jsonFormat2(PaintDemand)
  implicit val paintRequestJsonFormat = jsonFormat2(PaintRequest)
  implicit val paintRequestsJsonFormat = jsonFormat2(PaintRequests)
}

