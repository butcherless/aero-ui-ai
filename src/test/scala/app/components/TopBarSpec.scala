package app.components

import app.testkit.LaminarMountSpec
import org.scalajs.dom

class TopBarSpec extends LaminarMountSpec {

  it("renders the MartinAir logo image") {
    val root = renderRoot(TopBar())
    val logo = root.querySelector(".topbar-logo").asInstanceOf[dom.html.Image]
    logo.getAttribute("src") shouldBe "/martinair.png"
  }

  it("renders a real disabled Disconnect button, not just a grayed-out one") {
    val root = renderRoot(TopBar())
    val btn = root.querySelector(".topbar-disconnect").asInstanceOf[dom.html.Button]
    btn.textContent shouldBe "Disconnect"
    btn.disabled shouldBe true
  }

  it("renders an avatar image and the placeholder username") {
    val root = renderRoot(TopBar())
    root.querySelector(".topbar-avatar").tagName shouldBe "IMG"
    root.querySelector(".topbar-username").textContent shouldBe "Admin User"
  }
}
