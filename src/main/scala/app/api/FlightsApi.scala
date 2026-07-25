package app.api

import app.models._

import scala.concurrent.Future

object FlightsApi {
  private val base = "/api/v1/flights"

  def list(): Future[List[FlightDto]] =
    Http.getJsonList[FlightDto](base + Http.query("pageSize" -> Some("100")))

  def create(req: CreateFlightRequest): Future[FlightDto] =
    Http.postJson[CreateFlightRequest, FlightDto](base, req)

  def update(code: String, req: UpdateFlightRequest): Future[FlightDto] =
    Http.putJson[UpdateFlightRequest, FlightDto](s"$base/$code", req)

  def delete(code: String): Future[Unit] =
    Http.deleteReq(s"$base/$code")
}
