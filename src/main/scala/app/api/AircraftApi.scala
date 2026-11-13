package app.api

import app.models.*

import scala.concurrent.Future

object AircraftApi {
  private val base = "/api/v1/aircraft"

  def list(page: Int = 1): Future[List[AircraftDto]] =
    Http.getJsonList[AircraftDto](
      base + Http.paged(page)
    )

  def get(registration: String): Future[AircraftDto] =
    Http.getJson[AircraftDto](s"$base/$registration")

  def byAirline(icao: String, page: Int = 1): Future[List[AircraftDto]] =
    Http.getJsonList[AircraftDto](
      s"/api/v1/airlines/$icao/aircraft" +
        Http.paged(page)
    )

  def create(req: CreateAircraftRequest): Future[AircraftDto] =
    Http.postJson[CreateAircraftRequest, AircraftDto](base, req)

  def update(registration: String, req: UpdateAircraftRequest): Future[AircraftDto] =
    Http.putJson[UpdateAircraftRequest, AircraftDto](s"$base/$registration", req)

  def delete(registration: String): Future[Unit] =
    Http.deleteReq(s"$base/$registration")
}
