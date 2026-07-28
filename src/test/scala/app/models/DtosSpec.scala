package app.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import upickle.default._

class DtosSpec extends AnyFunSpec with Matchers {

  describe("CountryDto") {
    it("round-trips through JSON") {
      val country = CountryDto("ES", "Spain")
      read[CountryDto](write(country)) shouldBe country
    }
  }

  describe("AirlineDto") {
    it("omits absent optional fields when serialized") {
      val json = write(AirlineDto("IBE", "Iberia"))
      json should not include "alias"
      json should not include "callsign"
    }

    it("round-trips with optional fields present") {
      val airline = AirlineDto("IBE", "Iberia", alias = Some("Iberia Express"), callsign = Some("IBERIA"))
      read[AirlineDto](write(airline)) shouldBe airline
    }

    it("parses a backend response that omits optional fields as None") {
      // Matches the real API: AirlineDto responses never include alias/callsign when absent
      // rather than sending them as null — see [[api-quirks-aviation-openapi]] memory.
      val json = """{"icao":"AEA","name":"Air Europa"}"""
      read[AirlineDto](json) shouldBe AirlineDto("AEA", "Air Europa", None, None)
    }

    it("round-trips with iata present") {
      val airline = AirlineDto("IBE", "Iberia", iata = Some("IB"))
      read[AirlineDto](write(airline)) shouldBe airline
    }

    it("omits iata when absent, and parses a response that omits it as None") {
      val json = write(AirlineDto("IBE", "Iberia"))
      json should not include "iata"
      read[AirlineDto]("""{"icao":"IBE","name":"Iberia"}""").iata shouldBe None
    }
  }

  describe("CreateAirlineRequest / UpdateAirlineRequest") {
    it("round-trip with iata present") {
      val create = CreateAirlineRequest("IBE", "Iberia", "ES", iata = Some("IB"))
      read[CreateAirlineRequest](write(create)) shouldBe create

      val update = UpdateAirlineRequest("Iberia", "ES", iata = Some("IB"))
      read[UpdateAirlineRequest](write(update)) shouldBe update
    }
  }

  describe("AirportDto") {
    it("round-trips through JSON") {
      val airport = AirportDto("MAD", "LEMD", "Adolfo Suárez Madrid-Barajas", "Madrid")
      read[AirportDto](write(airport)) shouldBe airport
    }
  }

  describe("AircraftDto") {
    it("round-trips through JSON") {
      val aircraft = AircraftDto("EC-MIG", "B788", "Boeing 787-8 Dreamliner", "IBE")
      read[AircraftDto](write(aircraft)) shouldBe aircraft
    }
  }

  describe("FlightDto") {
    it("round-trips with alias absent") {
      val flight = FlightDto("IB3170", "09:15", "10:35", "MAD", "BCN", "IBE")
      read[FlightDto](write(flight)) shouldBe flight
    }

    it("round-trips with alias present") {
      val flight = FlightDto("UX9117", "07:05", "08:55", "MAD", "TFN", "AEA", alias = Some("AEA9117"))
      read[FlightDto](write(flight)) shouldBe flight
    }
  }

  describe("FlightInstanceDto") {
    it("round-trips through JSON") {
      val instance = FlightInstanceDto("id-1", "2024-06-28T15:23:00", "2024-06-28T19:41:00", "UX9117", "EC-MIG")
      read[FlightInstanceDto](write(instance)) shouldBe instance
    }
  }

  describe("LoginRequest / LoginResponse") {
    it("round-trip through JSON") {
      val req = LoginRequest("admin", "hunter2")
      read[LoginRequest](write(req)) shouldBe req

      val res = LoginResponse("tok-123", "Bearer", 3600)
      read[LoginResponse](write(res)) shouldBe res
    }
  }

  describe("HttpErrorResponse") {
    it("parses a minimal error body with no errors list") {
      val json = """{"message":"Country not found."}"""
      read[HttpErrorResponse](json) shouldBe HttpErrorResponse("Country not found.", None)
    }

    it("parses an error body that includes a validation errors list") {
      val json = """{"message":"Invalid value for: body","errors":["code: too short"]}"""
      read[HttpErrorResponse](json).errors shouldBe Some(List("code: too short"))
    }
  }

  describe("RouteDto") {
    it("round-trips distanceKm as an integer") {
      val route = RouteDto("MAD", "TFN", 1740)
      read[RouteDto](write(route)) shouldBe route
    }
  }
}
