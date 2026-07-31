package app.api

import app.models._

import scala.concurrent.Future

/** Routes are identified by (originIata, destinationIata); `list` supports an optional exact-3-letter `origin`/
  * `destination` filter, independently or combined — combining both returns at most one match, still as a list, empty
  * if that pair doesn't exist. There's still no get-by-key/update/delete for the route entity itself, only create, this
  * list/search endpoint, and the airline-association sub-resource endpoints.
  */
object RoutesApi {

  def list(origin: Option[String] = None, destination: Option[String] = None, page: Int = 1): Future[List[RouteDto]] =
    Http.getJsonList[RouteDto](
      "/api/v1/routes" + Http.query(
        "origin" -> origin,
        "destination" -> destination,
        "page" -> Some(page.toString),
        "pageSize" -> Some(Http.defaultPageSize.toString)
      )
    )

  def create(req: CreateRouteRequest): Future[RouteDto] =
    Http.postJson[CreateRouteRequest, RouteDto](s"/api/v1/routes", req)

  def airlinesOperating(origin: String, destination: String): Future[List[AirlineDto]] =
    Http.getJsonList[AirlineDto](s"/api/v1/routes/$origin/$destination/airlines")

  def associateAirline(origin: String, destination: String, icao: String): Future[Unit] =
    Http.postEmptyReq(s"/api/v1/routes/$origin/$destination/airlines/$icao")

  def disassociateAirline(origin: String, destination: String, icao: String): Future[Unit] =
    Http.deleteReq(s"/api/v1/routes/$origin/$destination/airlines/$icao")
}
