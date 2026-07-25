package app.api

import app.models._

import scala.concurrent.Future

object AirlinesApi {
  private val base = "/api/v1/airlines"

  def list(): Future[List[AirlineDto]] =
    Http.getJsonList[AirlineDto](base + Http.query("pageSize" -> Some("100")))

  def create(req: CreateAirlineRequest): Future[AirlineDto] =
    Http.postJson[CreateAirlineRequest, AirlineDto](base, req)

  def update(icao: String, req: UpdateAirlineRequest): Future[AirlineDto] =
    Http.putJson[UpdateAirlineRequest, AirlineDto](s"$base/$icao", req)

  def delete(icao: String): Future[Unit] =
    Http.deleteReq(s"$base/$icao")
}
