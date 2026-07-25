package app.components

import app.api.Http
import app.api.Http.given
import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

/** Selection state shared by every entity CRUD page: nothing selected, creating a new item, or editing an existing one.
  */
sealed trait DetailMode[+T]
object DetailMode {
  case object NoSelection extends DetailMode[Nothing]
  case object Creating extends DetailMode[Nothing]
  case class Editing[T](item: T) extends DetailMode[T]
}

/** Generic master-detail CRUD page shell shared by the straightforward entities (Countries, Airports, Airlines,
  * Aircraft, Flights): paginated loading with sample-data fallback, client-side search filtering (over whatever's
  * currently loaded), row selection, and wiring into MasterDetailShell. Each entity supplies only its API calls and its
  * create/edit form bodies, since the fields differ per entity.
  *
  * `serverSearch = true` additionally debounces the search box and re-fetches page 1 from `fetchPage` with the typed
  * query on every change (see `CountriesPage`) — for entities whose backend list endpoint has no query-based search,
  * leave it `false` (the default): `fetchPage` simply ignores the query it's given, and typing only narrows the
  * already-loaded page via `matchesSearch`, exactly as before. `minSearchLength` (only meaningful alongside
  * `serverSearch = true`) skips the debounced re-fetch entirely below that many characters — no request at all, not
  * even an unfiltered one — except when the box is cleared back to empty, which always re-fetches the plain list.
  */
object EntityCrudPage {

  def apply[T](
      title: String,
      searchPlaceholder: String,
      columns: List[(String, T => String)],
      rowKey: T => String,
      matchesSearch: (T, String) => Boolean,
      sampleData: List[T],
      fetchPage: (Int, String) => Future[List[T]],
      renderCreateForm: (T => Unit, () => Unit) => HtmlElement,
      renderEditForm: (T, T => Unit, () => Unit, () => Unit) => HtmlElement,
      emptySelectionHint: String,
      pageSize: Int = Http.defaultPageSize,
      serverSearch: Boolean = false,
      debounceMs: Int = 350,
      minSearchLength: Int = 0
  ): HtmlElement = {

    val itemsVar = Var(List.empty[T])
    val loadingVar = Var(true)
    val errorVar = Var(Option.empty[String])
    val searchVar = Var("")
    val activeQueryVar = Var("")
    val detailModeVar = Var[DetailMode[T]](DetailMode.NoSelection)
    val pageVar = Var(1)
    val hasNextVar = Var(false)

    def load(page: Int, query: String): Unit = {
      loadingVar.set(true)
      errorVar.set(None)
      fetchPage(page, query).onComplete {
        case Success(list) =>
          loadingVar.set(false)
          pageVar.set(page)
          activeQueryVar.set(query)
          hasNextVar.set(list.size >= pageSize)
          itemsVar.set(list)
        case Failure(_) =>
          loadingVar.set(false)
          errorVar.set(Some(Http.backendUnreachableMessage))
          pageVar.set(page)
          activeQueryVar.set(query)
          hasNextVar.set(false)
          itemsVar.set(sampleData)
      }
    }

    def goToPrevPage(): Unit = if (pageVar.now() > 1) load(pageVar.now() - 1, activeQueryVar.now())
    def goToNextPage(): Unit = if (hasNextVar.now()) load(pageVar.now() + 1, activeQueryVar.now())
    def runServerSearch(query: String): Unit = {
      val trimmed = query.trim
      if (serverSearch && (trimmed.isEmpty || trimmed.length >= minSearchLength)) load(1, query)
    }

    def filtered: Signal[List[T]] =
      itemsVar.signal.combineWith(searchVar.signal).map {
        case (items, q) =>
          val needle = q.trim.toLowerCase
          if (needle.isEmpty) items else items.filter(matchesSearch(_, needle))
      }

    val toolbar = div(
      cls := "entity-toolbar",
      input(
        cls := "search-input",
        placeholder := searchPlaceholder,
        controlled(value <-- searchVar.signal, onInput.mapToValue --> searchVar.writer),
        onInput(_.debounce(debounceMs).map(_.target.asInstanceOf[dom.html.Input].value)) -->
          Observer[String](runServerSearch)
      ),
      button(cls := "btn btn-add", "+ Add", onClick --> (_ => detailModeVar.set(DetailMode.Creating)))
    )

    val list = EntityTable[T](
      columns = columns,
      rows = filtered,
      rowKey = rowKey,
      selectedKey = detailModeVar.signal.map {
        case DetailMode.Editing(item) => Some(rowKey(item))
        case _ => None
      },
      onRowClick = item => detailModeVar.set(DetailMode.Editing(item)),
      loading = loadingVar.signal,
      error = errorVar.signal
    )

    val pagination = Pagination(pageVar.signal, hasNextVar.signal, goToPrevPage, goToNextPage)

    val detail: Signal[HtmlElement] = detailModeVar.signal.map {
      case DetailMode.NoSelection =>
        div(cls := "detail-placeholder", emptySelectionHint)

      case DetailMode.Creating =>
        renderCreateForm(
          created => { itemsVar.update(_ :+ created); detailModeVar.set(DetailMode.NoSelection) },
          () => detailModeVar.set(DetailMode.NoSelection)
        )

      case DetailMode.Editing(item) =>
        renderEditForm(
          item,
          updated => {
            itemsVar.update(_.map(i => if (rowKey(i) == rowKey(item)) updated else i))
            detailModeVar.set(DetailMode.NoSelection)
          },
          () => {
            itemsVar.update(_.filterNot(i => rowKey(i) == rowKey(item)))
            detailModeVar.set(DetailMode.NoSelection)
          },
          () => detailModeVar.set(DetailMode.NoSelection)
        )
    }

    MasterDetailShell(title, toolbar, div(list, pagination), detail).amend(onMountCallback(_ => load(1, "")))
  }
}
