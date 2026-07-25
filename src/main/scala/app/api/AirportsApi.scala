package app.api

import app.models._

import scala.concurrent.Future

object AirportsApi {
  private val base = "/api/v1/airports"

  def list(): Future[List[AirportDto]] =
    Http.getJsonList[AirportDto](base + Http.query("pageSize" -> Some("100")))

  def search(q: String): Future[List[AirportDto]] =
    Http.getJsonList[AirportDto](s"$base/search" + Http.query("q" -> Some(q)))

  def create(req: CreateAirportRequest): Future[AirportDto] =
    Http.postJson[CreateAirportRequest, AirportDto](base, req)

  def update(iata: String, req: UpdateAirportRequest): Future[AirportDto] =
    Http.putJson[UpdateAirportRequest, AirportDto](s"$base/$iata", req)

  def delete(iata: String): Future[Unit] =
    Http.deleteReq(s"$base/$iata")

  def countryOf(iata: String): Future[CountryDto] =
    Http.getJson[CountryDto](s"$base/$iata/country")
}
