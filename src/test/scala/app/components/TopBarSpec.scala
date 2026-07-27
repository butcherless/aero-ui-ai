package app.components

import app.auth.Session
import app.models.LoginResponse
import app.testkit.LaminarMountSpec
import org.scalajs.dom

class TopBarSpec extends LaminarMountSpec {

  it("renders the MartinAir logo image") {
    val root = renderRoot(TopBar())
    val logo = root.querySelector(".topbar-logo").asInstanceOf[dom.html.Image]
    logo.getAttribute("src") shouldBe "/martinair.png"
  }

  it("renders an avatar image") {
    Session.clear()
    val root = renderRoot(TopBar())
    root.querySelector(".topbar-avatar").tagName shouldBe "IMG"
  }

  it("shows the logged-in username and clears the session when Disconnect is clicked") {
    Session.store("admin", LoginResponse("tok", "Bearer", 3600))
    val root = renderRoot(TopBar())
    root.querySelector(".topbar-username").textContent shouldBe "admin"

    val btn = root.querySelector(".topbar-disconnect").asInstanceOf[dom.html.Button]
    btn.disabled shouldBe false

    btn.asInstanceOf[dom.html.Element].click()
    Session.sessionVar.now() shouldBe None
  }
}
