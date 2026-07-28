package app.components

import app.testkit.LaminarMountSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom

class PaginationSpec extends LaminarMountSpec {

  private def buttons(root: dom.html.Element): List[dom.html.Button] =
    toList(root.querySelectorAll("button")).map(_.asInstanceOf[dom.html.Button])

  it("disables Previous on page 1") {
    val root = renderRoot(Pagination(Val(1), Val(true), () => (), () => ()))
    buttons(root)(0).disabled shouldBe true
  }

  it("enables Previous beyond page 1") {
    val root = renderRoot(Pagination(Val(2), Val(true), () => (), () => ()))
    buttons(root)(0).disabled shouldBe false
  }

  it("disables Next when hasNext is false") {
    val root = renderRoot(Pagination(Val(1), Val(false), () => (), () => ()))
    buttons(root)(1).disabled shouldBe true
  }

  it("enables Next when hasNext is true") {
    val root = renderRoot(Pagination(Val(1), Val(true), () => (), () => ()))
    buttons(root)(1).disabled shouldBe false
  }

  it("shows the current page number") {
    val root = renderRoot(Pagination(Val(3), Val(true), () => (), () => ()))
    root.querySelector(".pagination-page").textContent shouldBe "Page 3"
  }

  it("invokes onPrev/onNext when their respective buttons are clicked") {
    var prevClicked = false
    var nextClicked = false
    val root = renderRoot(Pagination(Val(2), Val(true), () => prevClicked = true, () => nextClicked = true))

    val btns = buttons(root)
    btns(0).click()
    btns(1).click()

    prevClicked shouldBe true
    nextClicked shouldBe true
  }
}
