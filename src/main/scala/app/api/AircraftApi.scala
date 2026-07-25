package app.api

import app.models._

import scala.concurrent.Future

object AircraftApi {
  private val base = "/api/v1/aircraft"

  def list(): Future[List[AircraftDto]] =
    Http.getJsonList[AircraftDto](base + Http.query("pageSize" -> Some("100")))

  def create(req: CreateAircraftRequest): Future[AircraftDto] =
    Http.postJson[CreateAircraftRequest, AircraftDto](base, req)

  def update(registration: String, req: UpdateAircraftRequest): Future[AircraftDto] =
    Http.putJson[UpdateAircraftRequest, AircraftDto](s"$base/$registration", req)

  def delete(registration: String): Future[Unit] =
    Http.deleteReq(s"$base/$registration")
}
