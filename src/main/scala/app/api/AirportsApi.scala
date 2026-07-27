package app.api

import app.models._

import scala.concurrent.Future

object AirportsApi {
  private val base = "/api/v1/airports"

  def list(page: Int = 1): Future[List[AirportDto]] =
    Http.getJsonList[AirportDto](
      base + Http.query("page" -> Some(page.toString), "pageSize" -> Some(Http.defaultPageSize.toString))
    )

  def search(q: String): Future[List[AirportDto]] =
    Http.getJsonList[AirportDto](s"$base/search" + Http.query("q" -> Some(q)))

  def get(iata: String): Future[AirportDto] =
    Http.getJson[AirportDto](s"$base/$iata")

  def create(req: CreateAirportRequest): Future[AirportDto] =
    Http.postJson[CreateAirportRequest, AirportDto](base, req)

  def update(iata: String, req: UpdateAirportRequest): Future[AirportDto] =
    Http.putJson[UpdateAirportRequest, AirportDto](s"$base/$iata", req)

  def delete(iata: String): Future[Unit] =
    Http.deleteReq(s"$base/$iata")

  def countryOf(iata: String): Future[CountryDto] =
    Http.getJson[CountryDto](s"$base/$iata/country")
}
