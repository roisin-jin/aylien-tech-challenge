package com.example

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import scala.collection.parallel.CollectionConverters._

final case class PaintRequests(colors: Int, requests: Seq[PaintRequest])
final case class PaintRequest(customerId: Int, demands: Seq[PaintDemand])
final case class PaintDemand(color: Int, `type`: Int)

object PaintRequestValidation {

  val defaultErrorMsg = "Oops theres something wrong with your request"

  val errorCodeColor = StatusCodes.custom(460, "The valid number of total colors should be from 1 to 2000", defaultErrorMsg)
  val errorCodeDemands = StatusCodes.custom(461, "Total demands of request should not empty or exceed 3000", defaultErrorMsg)

  val errorCodePaintColorBuilder: Seq[Int] => StatusCode = customerIds =>
    StatusCodes.custom(462, s"Paint color can only be within range of 1 - 2000, found errors in request from customers: ${customerIds.mkString(",")}, ", defaultErrorMsg)
  val errorCodePaintTypeBuilder: Seq[Int] => StatusCode = customerIds =>
    StatusCodes.custom(463, s"Paint type can only be either 1 or 0, found errors in request from customers: ${customerIds.mkString(",")}, ", defaultErrorMsg)

  // Business logic check on request sent from users
  // Paint colors range: 1 <= N <= 2000
  // Total customers number: 1 <= M <= 2000
  // Total customers demands: T <= 3000
  // Paint type: 0 (gloss) or 1 (matt)
  def validate(request: PaintRequests): Option[StatusCode] = {
    if (request.colors < 1 || request.colors > 2000) {
      Some(errorCodeColor)
    } else if (request.requests.isEmpty || request.requests.map(_.demands.length).iterator.sum > 3000) {
      Some(errorCodeDemands)
    } else {
      val parRequests = request.requests.par

      val invalidPaintColors = parRequests.filter(q => q.customerId < 1 || q.customerId > 2000)
      if (invalidPaintColors.nonEmpty) {
        Some(errorCodePaintColorBuilder(invalidPaintColors.map(_.customerId).seq))
      } else {
        val malFormedPaintTypes = parRequests.filter(_.demands.exists(d => d.`type` != 0 || d.`type` != 1))
        if (malFormedPaintTypes.nonEmpty) {
          Some(errorCodePaintTypeBuilder(malFormedPaintTypes.map(_.customerId).seq))
        } else None
      }
    }
  }

}
