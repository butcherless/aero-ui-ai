package app.components

import app.testkit.LaminarMountSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.concurrent.Promise

/** Exercises the selection/create/edit state machine in isolation, using fake create/edit forms that call their
  * callbacks synchronously (as a real form's button click would). `fetchAll` is given a Promise that's never completed,
  * so the initial list load never lands and can't race with these assertions — the async fetch-then-render path is
  * covered separately in EntityCrudPageLoadSpec.
  */
class EntityCrudPageSpec extends LaminarMountSpec {

  private case class Item(id: String, name: String)

  private def buildPage(): dom.html.Element =
    renderRoot(
      EntityCrudPage[Item](
        title = "Items",
        searchPlaceholder = "Search…",
        columns = List("Name" -> (_.name)),
        rowKey = _.id,
        matchesSearch = (item, needle) => item.name.toLowerCase.contains(needle),
        sampleData = Nil,
        fetchAll = () => Promise[List[Item]]().future, // never resolves
        renderCreateForm = (onCreated, onCancel) =>
          div(
            button("do-create", onClick --> (_ => onCreated(Item("new", "New Item")))),
            button("do-cancel-create", onClick --> (_ => onCancel()))
          ),
        renderEditForm = (item, onSaved, onDeleted, onCancel) =>
          div(
            button("do-save", onClick --> (_ => onSaved(item.copy(name = item.name + " (edited)")))),
            button("do-delete", onClick --> (_ => onDeleted())),
            button("do-cancel-edit", onClick --> (_ => onCancel()))
          ),
        emptySelectionHint = "Nothing selected"
      )
    )

  private def clickButton(root: dom.html.Element, label: String): Unit =
    toList(root.querySelectorAll("button")).find(_.textContent == label) match {
      case Some(btn) => btn.asInstanceOf[dom.html.Element].click()
      case None => fail(s"""No button with text "$label" found""")
    }

  it("shows the empty-selection hint before anything is picked") {
    val root = buildPage()
    root.textContent should include("Nothing selected")
  }

  it("shows the create form after clicking Add, and adds the created item to the list") {
    val root = buildPage()

    clickButton(root, "+ Add")
    clickButton(root, "do-create")

    root.querySelector("tbody").textContent should include("New Item")
    // Back to the empty-selection hint after a successful create
    root.textContent should include("Nothing selected")
  }

  it("returns to the empty-selection hint when the create form is cancelled") {
    val root = buildPage()

    clickButton(root, "+ Add")
    clickButton(root, "do-cancel-create")

    root.textContent should include("Nothing selected")
    root.querySelector("tbody").textContent shouldBe "No results"
  }

  it("selects a row, shows the edit form, and applies a save back into the list") {
    val root = buildPage()

    clickButton(root, "+ Add")
    clickButton(root, "do-create")

    root.querySelector("tbody tr").asInstanceOf[dom.html.Element].click()
    clickButton(root, "do-save")

    root.querySelector("tbody").textContent should include("New Item (edited)")
    root.textContent should include("Nothing selected")
  }

  it("selects a row and removes it from the list on delete") {
    val root = buildPage()

    clickButton(root, "+ Add")
    clickButton(root, "do-create")

    root.querySelector("tbody tr").asInstanceOf[dom.html.Element].click()
    clickButton(root, "do-delete")

    root.querySelector("tbody").textContent shouldBe "No results"
  }

  it("highlights the selected row while editing") {
    val root = buildPage()

    clickButton(root, "+ Add")
    clickButton(root, "do-create")

    root.querySelector("tbody tr").asInstanceOf[dom.html.Element].click()

    root.querySelector("tbody tr").asInstanceOf[dom.html.Element].className should include("selected")
  }
}
