package app.models

import upickle.default.ReadWriter

// -- Countries --

case class CountryDto(code: String, name: String) derives ReadWriter
case class CreateCountryRequest(code: String, name: String) derives ReadWriter
case class UpdateCountryRequest(name: String) derives ReadWriter

// -- Airports --

case class AirportDto(iata: String, icaoCode: String, name: String, city: String) derives ReadWriter

case class CreateAirportRequest(
    iata: String,
    icaoCode: String,
    name: String,
    city: String,
    countryCode: String
) derives ReadWriter

case class UpdateAirportRequest(
    icaoCode: String,
    name: String,
    city: String,
    countryCode: String
) derives ReadWriter

// -- Airlines --

case class AirlineDto(
    icao: String,
    name: String,
    alias: Option[String] = None,
    callsign: Option[String] = None,
    iata: Option[String] = None
) derives ReadWriter

case class CreateAirlineRequest(
    icao: String,
    name: String,
    countryCode: String,
    alias: Option[String] = None,
    callsign: Option[String] = None,
    iata: Option[String] = None
) derives ReadWriter

case class UpdateAirlineRequest(
    name: String,
    countryCode: String,
    alias: Option[String] = None,
    callsign: Option[String] = None,
    iata: Option[String] = None
) derives ReadWriter

// -- Aircraft --

case class AircraftDto(
    registration: String,
    typeCode: String,
    description: String,
    airlineIcao: String
) derives ReadWriter

case class CreateAircraftRequest(
    registration: String,
    typeCode: String,
    description: String,
    airlineIcao: String
) derives ReadWriter

case class UpdateAircraftRequest(
    typeCode: String,
    description: String,
    airlineIcao: String
) derives ReadWriter

// -- Flights --

case class FlightDto(
    code: String,
    schedDeparture: String,
    schedArrival: String,
    originIata: String,
    destinationIata: String,
    airlineIcao: String,
    alias: Option[String] = None
) derives ReadWriter

case class CreateFlightRequest(
    code: String,
    schedDeparture: String,
    schedArrival: String,
    originIata: String,
    destinationIata: String,
    airlineIcao: String,
    alias: Option[String] = None
) derives ReadWriter

case class UpdateFlightRequest(
    schedDeparture: String,
    schedArrival: String,
    originIata: String,
    destinationIata: String,
    airlineIcao: String,
    alias: Option[String] = None
) derives ReadWriter

// -- Flight instances (read-only, no create/update/delete endpoints exist) --

case class FlightInstanceDto(
    id: String,
    departureDate: String,
    arrivalDate: String,
    flightCode: String,
    registration: String
) derives ReadWriter

// -- Routes (no list-all/get/update/delete endpoints; identified by origin+destination) --

case class RouteDto(
    originIata: String,
    destinationIata: String,
    distanceKm: Int,
    originAirportName: Option[String] = None,
    destinationAirportName: Option[String] = None
) derives ReadWriter
case class CreateRouteRequest(originIata: String, destinationIata: String, distanceKm: Int) derives ReadWriter

// -- Auth --

case class LoginRequest(username: String, password: String) derives ReadWriter
case class LoginResponse(token: String, tokenType: String, expiresIn: Int) derives ReadWriter

// -- Errors --

case class HttpErrorResponse(message: String, errors: Option[List[String]] = None) derives ReadWriter
