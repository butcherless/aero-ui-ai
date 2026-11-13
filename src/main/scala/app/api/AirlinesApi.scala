package app.api

import app.models.*

import scala.concurrent.Future

object AirlinesApi {
  private val base = "/api/v1/airlines"

  def list(page: Int = 1): Future[List[AirlineDto]] =
    Http.getJsonList[AirlineDto](
      base + Http.paged(page)
    )

  def search(q: String): Future[List[AirlineDto]] =
    Http.getJsonList[AirlineDto](s"$base/search" + Http.query("q" -> Some(q)))

  def byCountry(code: String, page: Int = 1): Future[List[AirlineDto]] =
    Http.getJsonList[AirlineDto](
      s"/api/v1/countries/$code/airlines" +
        Http.paged(page)
    )

  def get(icao: String): Future[AirlineDto] =
    Http.getJson[AirlineDto](s"$base/$icao")

  def create(req: CreateAirlineRequest): Future[AirlineDto] =
    Http.postJson[CreateAirlineRequest, AirlineDto](base, req)

  def update(icao: String, req: UpdateAirlineRequest): Future[AirlineDto] =
    Http.putJson[UpdateAirlineRequest, AirlineDto](s"$base/$icao", req)

  def delete(icao: String): Future[Unit] =
    Http.deleteReq(s"$base/$icao")
}
