package app.components

import app.api.Http
import app.api.Http.given
import com.raquo.laminar.api.L.*
import org.scalajs.dom

import scala.concurrent.Future

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

  /** The paginated-loading/search/sample-data-fallback state shared by `apply` and `readOnly` — the two only differ in
    * what they do with a row once it's selected (full CRUD vs. read-only field dump), not in how the list itself is
    * loaded, paged, or filtered.
    */
  private final case class LoadState[T](
      loader: PagedLoader[String, T],
      searchVar: Var[String],
      filtered: Signal[List[T]],
      runServerSearch: String => Unit
  )

  private def buildLoadState[T](
      fetchPage: (Int, String) => Future[List[T]],
      matchesSearch: (T, String) => Boolean,
      sampleData: List[T],
      pageSize: Int,
      serverSearch: Boolean,
      minSearchLength: Int
  ): LoadState[T] = {
    val loader = PagedLoader[String, T](fetchPage, sampleData, initialQuery = "", pageSize)
    val searchVar = Var("")

    def runServerSearch(query: String): Unit = {
      val trimmed = query.trim
      if (serverSearch && (trimmed.isEmpty || trimmed.length >= minSearchLength)) loader.load(1, query)
    }

    val filtered: Signal[List[T]] =
      loader.itemsVar.signal.combineWith(searchVar.signal).map {
        case (items, q) =>
          val needle = q.trim.toLowerCase
          if (needle.isEmpty) items else items.filter(matchesSearch(_, needle))
      }

    LoadState(loader, searchVar, filtered, runServerSearch)
  }

  private def searchBox(
      state: LoadState[?],
      searchPlaceholder: String,
      debounceMs: Int,
      onSearchFocus: () => Unit
  ): HtmlElement =
    input(
      cls := "search-input",
      placeholder := searchPlaceholder,
      controlled(value <-- state.searchVar.signal, onInput.mapToValue --> state.searchVar.writer),
      onFocus --> (_ => onSearchFocus()),
      onInput(_.debounce(debounceMs).map(_.target.asInstanceOf[dom.html.Input].value)) -->
        Observer[String](state.runServerSearch)
    )

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
      pageSize: Int = Http.defaultPageSize,
      serverSearch: Boolean = false,
      debounceMs: Int = 350,
      minSearchLength: Int = 0,
      renderExtraToolbar: (() => Unit, () => Unit) => List[HtmlElement] = (_, _) => Nil,
      onSearchFocus: () => Unit = () => ()
  ): HtmlElement = {

    val state = buildLoadState(fetchPage, matchesSearch, sampleData, pageSize, serverSearch, minSearchLength)
    import state.*

    val detailModeVar = Var[DetailMode[T]](DetailMode.NoSelection)

    val toolbar = div(
      cls := "entity-toolbar",
      searchBox(state, searchPlaceholder, debounceMs, onSearchFocus),
      renderExtraToolbar(() => loader.reload(), () => searchVar.set("")),
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
      loading = loader.loadingVar.signal,
      error = loader.errorVar.signal
    )

    val detail: Signal[Option[HtmlElement]] = detailModeVar.signal.map {
      case DetailMode.NoSelection =>
        None

      case DetailMode.Creating =>
        Some(
          renderCreateForm(
            created => { loader.itemsVar.update(_ :+ created); detailModeVar.set(DetailMode.NoSelection) },
            () => detailModeVar.set(DetailMode.NoSelection)
          )
        )

      case DetailMode.Editing(item) =>
        Some(
          renderEditForm(
            item,
            updated => {
              loader.itemsVar.update(_.map(i => if (rowKey(i) == rowKey(item)) updated else i))
              detailModeVar.set(DetailMode.NoSelection)
            },
            () => {
              loader.itemsVar.update(_.filterNot(i => rowKey(i) == rowKey(item)))
              detailModeVar.set(DetailMode.NoSelection)
            },
            () => detailModeVar.set(DetailMode.NoSelection)
          )
        )
    }

    MasterDetailShell(title, toolbar, div(list, loader.paginationBar), detail)
      .amend(onMountCallback(_ => loader.load(1, "")))
  }

  /** Read-only counterpart to `apply`: same paginated loading/search/sample-data-fallback behavior, but no "+ Add"
    * button and no write actions in the detail panel — clicking a row shows every column as a static field (via
    * `FormField.readOnly`) with just a Close button, exactly like the hand-rolled read-only detail view in
    * `FlightInstancesPage`. Used for the `/view/...` preview pages until the backend's real roles/permissions module
    * decides who gets write access.
    */
  def readOnly[T](
      title: String,
      searchPlaceholder: String,
      columns: List[(String, T => String)],
      rowKey: T => String,
      matchesSearch: (T, String) => Boolean,
      sampleData: List[T],
      fetchPage: (Int, String) => Future[List[T]],
      pageSize: Int = Http.defaultPageSize,
      serverSearch: Boolean = false,
      debounceMs: Int = 350,
      minSearchLength: Int = 0,
      renderExtraToolbar: (() => Unit, () => Unit) => List[HtmlElement] = (_, _) => Nil,
      onSearchFocus: () => Unit = () => (),
      detailFields: List[(String, T => String)] = Nil
  ): HtmlElement = {

    val state = buildLoadState(fetchPage, matchesSearch, sampleData, pageSize, serverSearch, minSearchLength)
    import state.*

    // Fields shown for a selected row; the table's columns unless the page wants more (e.g. an ID too long for a column)
    val fields = if (detailFields.isEmpty) columns else detailFields

    val selectedVar = Var(Option.empty[T])

    val toolbar = div(
      cls := "entity-toolbar",
      searchBox(state, searchPlaceholder, debounceMs, onSearchFocus),
      renderExtraToolbar(() => loader.reload(), () => searchVar.set(""))
    )

    val list = EntityTable[T](
      columns = columns,
      rows = filtered,
      rowKey = rowKey,
      selectedKey = selectedVar.signal.map(_.map(rowKey)),
      onRowClick = item => selectedVar.set(Some(item)),
      loading = loader.loadingVar.signal,
      error = loader.errorVar.signal
    )

    val detail: Signal[Option[HtmlElement]] = selectedVar.signal.map {
      case None =>
        None
      case Some(item) =>
        Some(
          div(
            cls := "detail-form",
            div(cls := "detail-heading", title),
            fields.map { case (label, extract) => FormField.readOnly(label, extract(item)) },
            FormActions.close(() => selectedVar.set(None))
          )
        )
    }

    MasterDetailShell(title, toolbar, div(list, loader.paginationBar), detail)
      .amend(onMountCallback(_ => loader.load(1, "")))
  }
}
