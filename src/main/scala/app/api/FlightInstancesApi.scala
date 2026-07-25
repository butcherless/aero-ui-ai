package app.api

import app.models._

import scala.concurrent.Future

/** Flight instances are read-only in this API: no create/update/delete endpoints exist. */
object FlightInstancesApi {
  private val base = "/api/v1/flight-instances"

  def list(): Future[List[FlightInstanceDto]] =
    Http.getJsonList[FlightInstanceDto](base + Http.query("pageSize" -> Some("100")))
}
