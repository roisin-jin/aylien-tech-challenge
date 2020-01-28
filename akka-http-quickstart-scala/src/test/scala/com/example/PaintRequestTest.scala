package com.example

import org.scalatest.FunSuite
import spray.json._
import JsonFormats._

class PaintRequestTest extends FunSuite {

  test("testConvertedInternalRequest") {
    val paintRequests = PaintRequest(1, Seq(PaintDemands(1, Seq(PaintDemand(1, 1))), PaintDemands(2, Seq(PaintDemand(1, 0)))))
    val expectedInternalRequestJson = """{"colors":1, "customers":2, "demands":[[1,1,1],[1,1,0]]}"""
    assertResult(expectedInternalRequestJson)(paintRequests.convertedInternalRequest.toJson.compactPrint)
  }

  test("invalid total colors") {
    val paintRequests = PaintRequest(2003, Seq(PaintDemands(1, Seq(PaintDemand(1, 1))), PaintDemands(2, Seq(PaintDemand(1, 0)))))
    assertResult(Some(PaintRequestValidation.errorCodeTotalColors))(PaintRequestValidation.validate(paintRequests))
  }

  test("invalid paint demand") {
    val paintRequests = PaintRequest(2, Seq(PaintDemands(1, Seq(PaintDemand(1, 3))), PaintDemands(2, Seq(PaintDemand(2, 0)))))
    assertResult(Some(PaintRequestValidation.errorCodeInvalidDemand(Set(1))))(PaintRequestValidation.validate(paintRequests))
  }
}
