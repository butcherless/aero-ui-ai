package app.auth

import app.models.LoginResponse
import org.scalajs.dom
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import upickle.default.write

class SessionSpec extends AnyFunSpec with Matchers {

  describe("store / clear") {
    it("stores the token, tokenType, username, and a computed expiresAt, then clears them") {
      Session.store("admin", LoginResponse("tok-123", "Bearer", 3600))

      Session.token shouldBe Some("tok-123")
      val data = Session.sessionVar.now().get
      data.username shouldBe "admin"
      data.tokenType shouldBe "Bearer"
      data.expiresAt should be > System.currentTimeMillis()

      Session.clear()
      Session.token shouldBe None
      Session.sessionVar.now() shouldBe None
    }
  }

  describe("loadFromStorage") {
    it("discards an already-expired stored session") {
      val expired = SessionData("tok", "Bearer", "admin", expiresAt = System.currentTimeMillis() - 1000)
      dom.window.localStorage.setItem("aero-ui-session", write(expired))

      Session.loadFromStorage() shouldBe None

      dom.window.localStorage.removeItem("aero-ui-session")
    }

    it("keeps a session that hasn't expired yet") {
      val valid = SessionData("tok", "Bearer", "admin", expiresAt = System.currentTimeMillis() + 60000)
      dom.window.localStorage.setItem("aero-ui-session", write(valid))

      Session.loadFromStorage() shouldBe Some(valid)

      dom.window.localStorage.removeItem("aero-ui-session")
    }
  }
}
