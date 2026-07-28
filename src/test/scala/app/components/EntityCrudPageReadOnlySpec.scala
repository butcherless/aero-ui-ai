package app.components

import app.testkit.LaminarAsyncMountSpec
import org.scalajs.dom

import scala.concurrent.Promise

/** Covers `EntityCrudPage.readOnly`, the counterpart to `EntityCrudPageSpec`/`EntityCrudPageLoadSpec` (which only
  * exercise `.apply`): no "+ Add" button, selecting a row shows static fields instead of an edit form, and Close
  * returns to the empty-selection hint. Async (like `EntityCrudPageLoadSpec`) since the initial list only appears once
  * `fetchPage`'s Future settles.
  */
class EntityCrudPageReadOnlySpec extends LaminarAsyncMountSpec {

  private case class Item(id: String, name: String)

  private def buildPage(promise: Promise[List[Item]]): dom.html.Element =
    renderRoot(
      EntityCrudPage.readOnly[Item](
        title = "Items",
        searchPlaceholder = "Search…",
        columns = List("Name" -> (_.name)),
        rowKey = _.id,
        matchesSearch = (item, needle) => item.name.toLowerCase.contains(needle),
        sampleData = Nil,
        fetchPage = (_, _) => promise.future,
        emptySelectionHint = "Nothing selected"
      )
    )

  it("shows the empty-selection hint and renders no \"+ Add\" button") {
    val promise = Promise[List[Item]]()
    val root = buildPage(promise)
    promise.success(List(Item("1", "Alpha")))

    promise.future.map { _ =>
      root.textContent should include("Nothing selected")
      toList(root.querySelectorAll("button")).map(_.textContent) should not contain "+ Add"
    }
  }

  it("selecting a row shows its fields as static text, and Close returns to the hint") {
    val promise = Promise[List[Item]]()
    val root = buildPage(promise)
    promise.success(List(Item("1", "Alpha")))

    promise.future.map { _ =>
      root.querySelector("tbody tr").asInstanceOf[dom.html.Element].click()

      val detail = root.querySelector(".entity-detail-panel")
      detail.textContent should include("Alpha")
      detail.querySelector("input") shouldBe null // read-only: no editable input, unlike the CRUD edit form

      toList(root.querySelectorAll("button")).find(_.textContent == "Close").get.asInstanceOf[dom.html.Element].click()

      root.textContent should include("Nothing selected")
    }
  }
}
