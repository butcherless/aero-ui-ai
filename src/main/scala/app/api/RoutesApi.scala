package app.api

import app.models._

import scala.concurrent.Future

/** Routes have no list-all/get-by-key/update/delete endpoints; they're identified by (originIata, destinationIata) and
  * browsed here by the airline operating them.
  */
object RoutesApi {

  def byAirline(icao: String): Future[List[RouteDto]] =
    Http.getJsonList[RouteDto](s"/api/v1/airlines/$icao/routes" + Http.query("pageSize" -> Some("100")))

  def create(req: CreateRouteRequest): Future[RouteDto] =
    Http.postJson[CreateRouteRequest, RouteDto](s"/api/v1/routes", req)

  def airlinesOperating(origin: String, destination: String): Future[List[AirlineDto]] =
    Http.getJsonList[AirlineDto](s"/api/v1/routes/$origin/$destination/airlines")

  def associateAirline(origin: String, destination: String, icao: String): Future[Unit] =
    Http.postEmptyReq(s"/api/v1/routes/$origin/$destination/airlines/$icao")

  def disassociateAirline(origin: String, destination: String, icao: String): Future[Unit] =
    Http.deleteReq(s"/api/v1/routes/$origin/$destination/airlines/$icao")
}
