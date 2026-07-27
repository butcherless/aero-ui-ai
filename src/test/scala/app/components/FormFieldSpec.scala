package app.components

import app.testkit.LaminarMountSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom

class FormFieldSpec extends LaminarMountSpec {

  describe("FormField.text") {

    it("renders the label and the Var's current value") {
      val nameVar = Var("Spain")
      val root = renderRoot(FormField.text("Name", nameVar))

      root.querySelector(".form-field-label").textContent shouldBe "Name"
      root.querySelector("input").asInstanceOf[dom.html.Input].value shouldBe "Spain"
    }

    it("writes user input back into the bound Var") {
      val nameVar = Var("Spain")
      val root = renderRoot(FormField.text("Name", nameVar))
      val input = root.querySelector("input").asInstanceOf[dom.html.Input]

      input.value = "France"
      input.dispatchEvent(new dom.Event("input"))

      nameVar.now() shouldBe "France"
    }

    it("reflects external Var updates back into the input") {
      val nameVar = Var("Spain")
      val root = renderRoot(FormField.text("Name", nameVar))

      nameVar.set("Portugal")

      root.querySelector("input").asInstanceOf[dom.html.Input].value shouldBe "Portugal"
    }
  }

  describe("FormField.number") {
    it("renders an <input type=number>") {
      val root = renderRoot(FormField.number("Distance (km)", Var("1740")))
      root.querySelector("input").getAttribute("type") shouldBe "number"
    }
  }

  describe("FormField.password") {
    it("renders an <input type=password> and writes back into the bound Var") {
      val passwordVar = Var("")
      val root = renderRoot(FormField.password("Password", passwordVar))
      val input = root.querySelector("input").asInstanceOf[dom.html.Input]

      input.getAttribute("type") shouldBe "password"

      input.value = "hunter2"
      input.dispatchEvent(new dom.Event("input"))

      passwordVar.now() shouldBe "hunter2"
    }
  }

  describe("FormField.readOnly") {
    it("renders static text, not an input") {
      val root = renderRoot(FormField.readOnly("Code", "ES"))

      root.querySelector("input") shouldBe null
      root.textContent should include("ES")
    }
  }

  describe("FormField.errorBanner") {
    it("renders nothing while empty, then the message once the Var is set") {
      val errVar = Var(Option.empty[String])
      val root = renderRoot(FormField.errorBanner(errVar))

      root.textContent shouldBe ""

      errVar.set(Some("Something went wrong"))
      root.textContent shouldBe "Something went wrong"

      errVar.set(None)
      root.textContent shouldBe ""
    }
  }
}
