package app.api

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class HttpSpec extends AnyFunSpec with Matchers {

  describe("Http.query") {
    it("returns an empty string when all params are absent or empty") {
      Http.query("name" -> None, "page" -> Some("")) shouldBe ""
    }

    it("builds a query string from present, non-empty params") {
      Http.query("name" -> Some("Madrid"), "page" -> Some("1")) shouldBe "?name=Madrid&page=1"
    }

    it("drops absent params but keeps present ones") {
      Http.query("name" -> None, "page" -> Some("2")) shouldBe "?page=2"
    }

    it("URL-encodes special characters") {
      Http.query("q" -> Some("São Paulo")) shouldBe "?q=S%C3%A3o%20Paulo"
    }
  }
}
