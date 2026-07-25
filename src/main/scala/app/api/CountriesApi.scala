package app.api

import app.models._

import scala.concurrent.Future

object CountriesApi {
  private val base = "/api/v1/countries"

  def list(name: Option[String] = None): Future[List[CountryDto]] =
    Http.getJsonList[CountryDto](base + Http.query("name" -> name, "pageSize" -> Some("100")))

  def create(req: CreateCountryRequest): Future[CountryDto] =
    Http.postJson[CreateCountryRequest, CountryDto](base, req)

  def update(code: String, req: UpdateCountryRequest): Future[CountryDto] =
    Http.putJson[UpdateCountryRequest, CountryDto](s"$base/$code", req)

  def delete(code: String): Future[Unit] =
    Http.deleteReq(s"$base/$code")
}
