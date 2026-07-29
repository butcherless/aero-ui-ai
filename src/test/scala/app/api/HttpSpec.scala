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

  describe("Http.loadFailure") {
    it("surfaces the backend's own message verbatim for an ApiError, and skips the sample-data fallback") {
      val failure = Http.loadFailure(Http.ApiError(404, "Country not found: AA"))
      failure.message shouldBe "Country not found: AA"
      failure.useSampleData shouldBe false
    }

    it("falls back to the generic unreachable message and sample data for a true network failure") {
      val failure = Http.loadFailure(new RuntimeException("network down"))
      failure.message shouldBe Http.backendUnreachableMessage
      failure.useSampleData shouldBe true
    }
  }
}
