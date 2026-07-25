package app.components

import app.testkit.LaminarMountSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom

class EntityTableSpec extends LaminarMountSpec {

  private case class Item(id: String, name: String)

  it("renders a header row and one row per item") {
    val root = renderRoot(
      EntityTable[Item](
        columns = List("ID" -> (_.id), "Name" -> (_.name)),
        rows = Val(List(Item("1", "Alpha"), Item("2", "Beta"))),
        rowKey = _.id,
        selectedKey = Val(None),
        onRowClick = _ => ()
      )
    )

    toList(root.querySelectorAll("thead th")).map(_.textContent) shouldBe List("ID", "Name")

    val rows = toList(root.querySelectorAll("tbody tr"))
    rows should have length 2
    rows(0).textContent should include("Alpha")
    rows(1).textContent should include("Beta")
  }

  it("shows an empty-state row when there are no items") {
    val root = renderRoot(
      EntityTable[Item](
        columns = List("ID" -> (_.id)),
        rows = Val(Nil),
        rowKey = _.id,
        selectedKey = Val(None),
        onRowClick = _ => ()
      )
    )

    root.querySelector("tbody").textContent shouldBe "No results"
  }

  it("highlights only the row matching the selected key") {
    val root = renderRoot(
      EntityTable[Item](
        columns = List("ID" -> (_.id)),
        rows = Val(List(Item("1", "Alpha"), Item("2", "Beta"))),
        rowKey = _.id,
        selectedKey = Val(Some("2")),
        onRowClick = _ => ()
      )
    )

    val rows = toList(root.querySelectorAll("tbody tr")).map(_.asInstanceOf[dom.html.Element])
    rows(0).className should not include "selected"
    rows(1).className should include("selected")
  }

  it("invokes onRowClick with the clicked item") {
    var clicked: Option[Item] = None
    val root = renderRoot(
      EntityTable[Item](
        columns = List("ID" -> (_.id)),
        rows = Val(List(Item("1", "Alpha"))),
        rowKey = _.id,
        selectedKey = Val(None),
        onRowClick = item => clicked = Some(item)
      )
    )

    root.querySelector("tbody tr").asInstanceOf[dom.html.Element].click()

    clicked shouldBe Some(Item("1", "Alpha"))
  }

  it("shows a loading indicator when loading is true") {
    val root = renderRoot(
      EntityTable[Item](
        columns = List("ID" -> (_.id)),
        rows = Val(Nil),
        rowKey = _.id,
        selectedKey = Val(None),
        onRowClick = _ => (),
        loading = Val(true)
      )
    )

    root.textContent should include("Loading")
  }

  it("shows an error banner when error is set, taking priority over the loading indicator") {
    val root = renderRoot(
      EntityTable[Item](
        columns = List("ID" -> (_.id)),
        rows = Val(Nil),
        rowKey = _.id,
        selectedKey = Val(None),
        onRowClick = _ => (),
        loading = Val(true),
        error = Val(Some("boom"))
      )
    )

    root.textContent should include("boom")
    root.textContent should not include "Loading"
  }
}
