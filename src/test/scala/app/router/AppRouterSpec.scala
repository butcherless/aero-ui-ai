package app.router

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class AppRouterSpec extends AnyFunSpec with Matchers {

  describe("Page.toPath / fromPath") {
    it("round-trips every page through its path") {
      Page.values.foreach { page =>
        Page.fromPath(Page.toPath(page)) shouldBe page
      }
    }

    it("maps Countries to the root path") {
      Page.toPath(Page.Countries) shouldBe "/"
    }

    it("gives every page its own path") {
      Page.values.map(_.path).distinct should have size Page.values.length.toLong
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
