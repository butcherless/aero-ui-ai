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
