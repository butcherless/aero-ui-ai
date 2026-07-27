package app.api

import app.models._

import scala.concurrent.Future

object AirlinesApi {
  private val base = "/api/v1/airlines"

  def list(page: Int = 1): Future[List[AirlineDto]] =
    Http.getJsonList[AirlineDto](
      base + Http.query("page" -> Some(page.toString), "pageSize" -> Some(Http.defaultPageSize.toString))
    )

  def search(q: String): Future[List[AirlineDto]] =
    Http.getJsonList[AirlineDto](s"$base/search" + Http.query("q" -> Some(q)))

  def get(icao: String): Future[AirlineDto] =
    Http.getJson[AirlineDto](s"$base/$icao")

  def create(req: CreateAirlineRequest): Future[AirlineDto] =
    Http.postJson[CreateAirlineRequest, AirlineDto](base, req)

  def update(icao: String, req: UpdateAirlineRequest): Future[AirlineDto] =
    Http.putJson[UpdateAirlineRequest, AirlineDto](s"$base/$icao", req)

  def delete(icao: String): Future[Unit] =
    Http.deleteReq(s"$base/$icao")
}
