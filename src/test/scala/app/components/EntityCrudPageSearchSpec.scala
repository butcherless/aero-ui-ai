package app.components

import app.testkit.LaminarAsyncMountSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.js

/** Covers EntityCrudPage's opt-in debounced server search (`serverSearch = true`), used by CountriesPage: typing should
  * re-fetch page 1 with the typed query after the debounce interval elapses. `debounceMs` is set very low here so the
  * tests stay fast, and `delay` waits comfortably past it using a real timer (Airstream's `debounce` runs on real JS
  * timers, not a virtual clock).
  */
class EntityCrudPageSearchSpec extends LaminarAsyncMountSpec {

  private case class Item(id: String, name: String)

  private def delay(ms: Int): Future[Unit] = {
    val p = Promise[Unit]()
    js.timers.setTimeout(ms)(p.success(()))
    p.future
  }

  private def buildPage(
      serverSearch: Boolean,
      fetchPage: (Int, String) => Future[List[Item]],
      minSearchLength: Int = 0
  ): dom.html.Element =
    renderRoot(
      EntityCrudPage[Item](
        title = "Items",
        searchPlaceholder = "Search…",
        columns = List("Name" -> (_.name)),
        rowKey = _.id,
        matchesSearch = (item, needle) => item.name.toLowerCase.contains(needle),
        sampleData = Nil,
        fetchPage = fetchPage,
        renderCreateForm = (_, _) => div(),
        renderEditForm = (_, _, _, _) => div(),
        emptySelectionHint = "Nothing selected",
        serverSearch = serverSearch,
        debounceMs = 20,
        minSearchLength = minSearchLength
      )
    )

  private def typeInto(root: dom.html.Element, text: String): Unit = {
    val input = root.querySelector(".search-input").asInstanceOf[dom.html.Input]
    input.value = text
    input.dispatchEvent(new dom.Event("input"))
  }

  it("does not re-fetch on typing when serverSearch is disabled (default)") {
    var calls = List.empty[(Int, String)]
    val root = buildPage(
      serverSearch = false,
      fetchPage = (page, query) => { calls = calls :+ (page -> query); Future.successful(List(Item("1", "Alpha"))) }
    )

    typeInto(root, "abc")

    delay(60).map { _ =>
      calls shouldBe List(1 -> "")
    }
  }

  it("debounces and re-fetches page 1 with the typed query when serverSearch is enabled") {
    var calls = List.empty[(Int, String)]
    val root = buildPage(
      serverSearch = true,
      fetchPage = (page, query) => { calls = calls :+ (page -> query); Future.successful(List(Item("1", "Alpha"))) }
    )

    typeInto(root, "fra")

    delay(60).map { _ =>
      calls shouldBe List(1 -> "", 1 -> "fra")
    }
  }

  it("does not fetch at all for queries shorter than minSearchLength") {
    var calls = List.empty[(Int, String)]
    val root = buildPage(
      serverSearch = true,
      fetchPage = (page, query) => { calls = calls :+ (page -> query); Future.successful(List(Item("1", "Alpha"))) },
      minSearchLength = 3
    )

    typeInto(root, "f")

    delay(60).map { _ =>
      calls shouldBe List(1 -> "") // only the initial mount load — the short query never triggered a fetch
    }
  }

  it("fetches once the query reaches minSearchLength, and again when cleared back to empty") {
    var calls = List.empty[(Int, String)]
    val root = buildPage(
      serverSearch = true,
      fetchPage = (page, query) => { calls = calls :+ (page -> query); Future.successful(List(Item("1", "Alpha"))) },
      minSearchLength = 3
    )

    typeInto(root, "fr")
    delay(60).flatMap { _ =>
      typeInto(root, "fra")
      delay(60).flatMap { _ =>
        typeInto(root, "")
        delay(60).map { _ =>
          calls shouldBe List(1 -> "", 1 -> "fra", 1 -> "")
        }
      }
    }
  }
}
