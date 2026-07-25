package app.components

import app.testkit.LaminarAsyncMountSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.concurrent.Future
import scala.concurrent.Promise

/** Covers the async fetch-then-render path in EntityCrudPage: since `load()` registers its `.onComplete` callback on
  * the exact Future/Promise handed back by `fetchAll`, driving that same Promise here and chaining assertions off of
  * `promise.future` guarantees our assertions run strictly after EntityCrudPage's own state update — Future callbacks
  * on one Promise fire in registration order, and EntityCrudPage's callback was registered first (synchronously, during
  * `renderRoot`, since Laminar mounts synchronously).
  */
class EntityCrudPageLoadSpec extends LaminarAsyncMountSpec {

  private case class Item(id: String, name: String)

  private def buildPage(fetchAll: () => Future[List[Item]], sampleData: List[Item] = Nil): dom.html.Element =
    renderRoot(
      EntityCrudPage[Item](
        title = "Items",
        searchPlaceholder = "Search…",
        columns = List("Name" -> (_.name)),
        rowKey = _.id,
        matchesSearch = (item, needle) => item.name.toLowerCase.contains(needle),
        sampleData = sampleData,
        fetchAll = fetchAll,
        renderCreateForm = (_, _) => div(),
        renderEditForm = (_, _, _, _) => div(),
        emptySelectionHint = "Nothing selected"
      )
    )

  it("renders the fetched items once the load succeeds") {
    val promise = Promise[List[Item]]()
    val root = buildPage(fetchAll = () => promise.future)

    promise.success(List(Item("1", "Alpha"), Item("2", "Beta")))

    promise.future.map { _ =>
      root.querySelector("tbody").textContent should include("Alpha")
      root.querySelector("tbody").textContent should include("Beta")
    }
  }

  it("falls back to sample data with an error banner when the load fails") {
    val promise = Promise[List[Item]]()
    val root = buildPage(fetchAll = () => promise.future, sampleData = List(Item("s", "Sample Item")))

    promise.failure(new RuntimeException("network down"))

    // transformWith (not map) since this branch settles the future as a failure.
    promise.future.transformWith(_ => Future.successful(())).map { _ =>
      root.querySelector("tbody").textContent should include("Sample Item")
      root.textContent should include("Could not connect to the backend")
    }
  }
}
