package app.components

import app.testkit.LaminarMountSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom

class FormActionsSpec extends LaminarMountSpec {

  private def buttons(root: dom.html.Element): List[dom.html.Button] =
    toList(root.querySelectorAll("button")).map(_.asInstanceOf[dom.html.Button])

  describe("FormActions.saveDeleteCancel") {
    it("renders Save/Delete/Cancel and wires each click handler") {
      var saved = false
      var deleted = false
      var cancelled = false
      val root = renderRoot(
        FormActions.saveDeleteCancel(Val(false), () => saved = true, () => deleted = true, () => cancelled = true)
      )

      val btns = buttons(root)
      btns.map(_.textContent) shouldBe List("Save", "Delete", "Cancel")

      btns(0).click()
      btns(1).click()
      btns(2).click()

      saved shouldBe true
      deleted shouldBe true
      cancelled shouldBe true
    }

    it("disables Save and Delete while saving, but leaves Cancel enabled") {
      val root = renderRoot(FormActions.saveDeleteCancel(Val(true), () => (), () => (), () => ()))
      val btns = buttons(root)

      btns(0).disabled shouldBe true
      btns(1).disabled shouldBe true
      btns(2).disabled shouldBe false
    }
  }

  describe("FormActions.saveCancel") {
    it("renders Save/Cancel and wires each click handler") {
      var saved = false
      var cancelled = false
      val root = renderRoot(FormActions.saveCancel(Val(false), () => saved = true, () => cancelled = true))

      val btns = buttons(root)
      btns.map(_.textContent) shouldBe List("Save", "Cancel")

      btns(0).click()
      btns(1).click()

      saved shouldBe true
      cancelled shouldBe true
    }

    it("disables Save while saving") {
      val root = renderRoot(FormActions.saveCancel(Val(true), () => (), () => ()))
      buttons(root)(0).disabled shouldBe true
    }
  }

  describe("FormActions.close") {
    it("renders a Close button and wires its click handler") {
      var closed = false
      val root = renderRoot(FormActions.close(() => closed = true))

      val btns = buttons(root)
      btns.map(_.textContent) shouldBe List("Close")

      btns(0).click()
      closed shouldBe true
    }
  }
}
