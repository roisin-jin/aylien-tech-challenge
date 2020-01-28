package com.example

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }

case class InternalRequest(colors: Int, customers: Int, demands: Seq[Seq[Int]]) {
  lazy val convertedPaintRequest: PaintRequest = PaintRequest(colors, demands.iterator.map(d =>
    PaintDemands(demands.indexOf(d), d.tail.sliding(2, 2).map(x => PaintDemand(x(0), x(1))).toSeq)).toSeq)
}

final case class PaintRequest(totalColors: Int, customerDemands: Seq[PaintDemands]) {
  lazy val customersDemandsMap: Map[Int, Seq[PaintDemand]] = customerDemands.groupBy(_.customerId).view.mapValues(_.flatMap(_.demands)).toMap
  lazy val convertedInternalRequest: InternalRequest = {
    InternalRequest(colors = totalColors, customers = customersDemandsMap.keySet.size,
      demands = customersDemandsMap.map(p => p._2.length +: p._2.flatMap(d => Seq(d.color, d.`type`))).toSeq)
  }
}

final case class PaintDemands(customerId: Int, demands: Seq[PaintDemand])
final case class PaintDemand(color: Int, `type`: Int)

object PaintRequestValidation {

  val defaultErrorMsg = "Oops theres something wrong with your request"

  val errorCodeTotalColors = StatusCodes.custom(460, "Total number of colors should be from 1 to 2000", defaultErrorMsg)
  val errorCodeTotalCustomers = StatusCodes.custom(461, "Total number of customers should not exceed 2000", defaultErrorMsg)
  val errorCodeTotalDemands = StatusCodes.custom(462, "Total number of demands should not exceed 3000", defaultErrorMsg)

  val errorCodeInvalidDemand: Set[Int] => StatusCode = customerIds => StatusCodes.custom(463,
    s"Paint color number ranges from 1 to 2000 while type can only be either 1 or 0, found errors in request from customers: ${customerIds.mkString(",")}",
    defaultErrorMsg)

  // Business logic check on request sent from users
  // Paint colors range: 1 <= N <= 2000
  // Total customers number: 1 <= M <= 2000
  // Total customers demands: 0 <= T <= 3000
  // Paint type: 0 (gloss) or 1 (matt)
  def validate(request: PaintRequest): Option[StatusCode] = {
    if (request.totalColors < 1 || request.totalColors > 2000) {
      Some(errorCodeTotalColors)
    } else if (request.customersDemandsMap.keySet.size > 2000) {
      Some(errorCodeTotalCustomers)
    } else if (request.customersDemandsMap.values.map(_.length).sum > 3000) {
      Some(errorCodeTotalDemands)
    } else {

      val invalidPaintDemands = request.customersDemandsMap.filter(_._2.exists(d =>
        d.color < 1 || d.color > 2000 || (d.`type` != 0 && d.`type` != 1)))

      if (invalidPaintDemands.nonEmpty) {
        Some(errorCodeInvalidDemand(invalidPaintDemands.keySet))
      } else {
        None
      }
    }
  }

}
