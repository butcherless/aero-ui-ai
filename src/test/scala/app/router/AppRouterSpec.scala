package app.router

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class AppRouterSpec extends AnyFunSpec with Matchers {

  private val allPages = List(
    Page.Countries,
    Page.Airports,
    Page.Airlines,
    Page.Aircraft,
    Page.Flights,
    Page.FlightInstances,
    Page.Routes,
    Page.Profile,
    Page.ViewCountries,
    Page.ViewAirports,
    Page.ViewAirlines,
    Page.ViewAircraft,
    Page.ViewFlights,
    Page.ViewRoutes,
    Page.Login
  )

  describe("Page.toPath / fromPath") {
    it("round-trips every page through its path") {
      allPages.foreach { page =>
        Page.fromPath(Page.toPath(page)) shouldBe page
      }
    }

    it("maps Countries to the root path") {
      Page.toPath(Page.Countries) shouldBe "/"
    }

    it("falls back to Countries for an unknown path") {
      Page.fromPath("/does-not-exist") shouldBe Page.Countries
    }

    it("distinguishes each entity's normal path from its /view/ counterpart") {
      Page.fromPath("/airports") shouldBe Page.Airports
      Page.fromPath("/view/airports") shouldBe Page.ViewAirports
    }
  }
}
