package com.example.util

import com.example.util.JsonFormats._
import org.scalatest.FunSuite
import spray.json._

class PaintRequestValidaterTest extends FunSuite {

  test("testConvertedInternalRequest") {
    val paintRequests = PaintRequest(1, Seq(PaintDemands(1, Seq(PaintDemand(1, 1))), PaintDemands(2, Seq(PaintDemand(1, 0)))))
    val expectedInternalRequestJson = """{"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}"""
    assertResult(expectedInternalRequestJson)(paintRequests.getConvertedInternalRequest.toJson.compactPrint)
  }

  test("invalid total colors") {
    val paintRequests = PaintRequest(2003, Seq(PaintDemands(1, Seq(PaintDemand(1, 1))), PaintDemands(2, Seq(PaintDemand(1, 0)))))
    assertResult(Some(PaintRequestValidater.errorCodeTotalColors))(PaintRequestValidater.validate(paintRequests))
  }

  test("invalid paint demand") {
    val paintRequests = PaintRequest(2, Seq(PaintDemands(1, Seq(PaintDemand(1, 3))), PaintDemands(2, Seq(PaintDemand(2, 0)))))
    assertResult(Some(PaintRequestValidater.errorCodeInvalidDemand(Set(1))))(PaintRequestValidater.validate(paintRequests))
  }
}
