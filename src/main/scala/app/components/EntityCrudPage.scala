package app.components

import app.api.Http
import app.api.Http.given
import com.raquo.laminar.api.L.*

import scala.concurrent.Future

/** Selection state shared by every entity CRUD page: nothing selected, creating a new item, or editing an existing one.
  */
enum DetailMode[+T] {
  case NoSelection extends DetailMode[Nothing]
  case Creating extends DetailMode[Nothing]
  case Editing[T](item: T) extends DetailMode[T]

  /** The item being edited, if any: what the list highlights as selected. */
  def editing: Option[T] = this match {
    case Editing(item) => Some(item)
    case NoSelection | Creating => None
  }
}

/** How an entity's list is shown, loaded and searched. A page defines it once and passes it to both
  * `EntityCrudPage.apply` and `EntityCrudPage.readOnly`.
  *
  * `serverSearch = true` additionally debounces the search box and re-fetches page 1 from `fetchPage` with the typed
  * query on every change (see `CountriesPage`). For entities whose backend list endpoint has no query-based search,
  * leave it `false` (the default): `fetchPage` ignores the query it's given, and typing only narrows the already-loaded
  * page via `matchesSearch`. `minSearchLength` (only meaningful with `serverSearch = true`) skips the debounced
  * re-fetch below that many characters, except when the box is cleared back to empty, which always re-fetches the plain
  * list.
  *
  * `renderExtraToolbar` receives `reload` (re-fetch page 1 of the last query) and `clearSearch` (empty the main search
  * box), for pages with secondary filters.
  */
final case class EntityListSpec[T](
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
    onSearchFocus: () => Unit = () => ()
)

/** Generic master-detail page shared by the straightforward entities (Countries, Airports, Airlines, Aircraft, Flights,
  * and read-only Flight Instances): paginated loading with sample-data fallback, client-side search filtering over the
  * loaded page, row selection, and wiring into MasterDetailShell. Each entity supplies an `EntityListSpec` and, for
  * full CRUD, its create/edit form bodies.
  */
object EntityCrudPage {

  /** The loading/search state shared by `apply` and `readOnly`: the two only differ in what they do with a selected row
    * (full CRUD vs. a read-only field dump), not in how the list is loaded, paged or filtered.
    */
  private final class ListState[T](spec: EntityListSpec[T]) {
    val loader: PagedLoader[String, T] =
      PagedLoader[String, T](spec.fetchPage, spec.sampleData, initialQuery = "", spec.pageSize)
    val searchVar: Var[String] = Var("")

    private def runServerSearch(query: String): Unit = {
      val trimmed = query.trim
      if (spec.serverSearch && (trimmed.isEmpty || trimmed.length >= spec.minSearchLength)) loader.load(1, query)
    }

    val filtered: Signal[List[T]] =
      loader.itemsVar.signal.combineWith(searchVar.signal).map {
        case (items, q) =>
          val needle = q.trim.toLowerCase
          if (needle.isEmpty) items else items.filter(spec.matchesSearch(_, needle))
      }

    val searchBox: HtmlElement =
      input(
        cls := "search-input",
        placeholder := spec.searchPlaceholder,
        controlled(value <-- searchVar.signal, onInput.mapToValue --> searchVar.writer),
        onFocus --> (_ => spec.onSearchFocus()),
        onInput.mapToValue.compose(_.debounce(spec.debounceMs)) -->
          Observer[String](runServerSearch)
      )
  }

  /** Toolbar, table, pagination and detail panel, loading page 1 on mount. `toolbarActions` go after the search box and
    * any extra toolbar controls.
    */
  private def shell[T](
      spec: EntityListSpec[T],
      state: ListState[T],
      selectedKey: Signal[Option[String]],
      onRowClick: T => Unit,
      detail: Signal[Option[HtmlElement]],
      toolbarActions: List[HtmlElement]
  ): HtmlElement = {
    val toolbar = div(
      cls := "entity-toolbar",
      state.searchBox,
      spec.renderExtraToolbar(() => state.loader.reload(), () => state.searchVar.set("")),
      toolbarActions
    )

    val list = EntityTable[T](
      columns = spec.columns,
      rows = state.filtered,
      rowKey = spec.rowKey,
      selectedKey = selectedKey,
      onRowClick = onRowClick,
      loading = state.loader.loadingVar.signal,
      error = state.loader.errorVar.signal
    )

    MasterDetailShell(spec.title, toolbar, div(list, state.loader.paginationBar), detail)
      .amend(onMountCallback(_ => state.loader.load(1, "")))
  }

  def apply[T](
      spec: EntityListSpec[T],
      renderCreateForm: (T => Unit, () => Unit) => HtmlElement,
      renderEditForm: (T, T => Unit, () => Unit, () => Unit) => HtmlElement
  ): HtmlElement = {
    val state = ListState(spec)
    val items = state.loader.itemsVar
    val detailModeVar = Var[DetailMode[T]](DetailMode.NoSelection)
    def closeDetail(): Unit = detailModeVar.set(DetailMode.NoSelection)
    def sameKey(a: T, b: T): Boolean = spec.rowKey(a) == spec.rowKey(b)

    val detail: Signal[Option[HtmlElement]] = detailModeVar.signal.map {
      case DetailMode.NoSelection =>
        None

      case DetailMode.Creating =>
        Some(renderCreateForm(created => { items.update(_ :+ created); closeDetail() }, () => closeDetail()))

      case DetailMode.Editing(item) =>
        Some(
          renderEditForm(
            item,
            updated => { items.update(_.map(i => if (sameKey(i, item)) updated else i)); closeDetail() },
            () => { items.update(_.filterNot(sameKey(_, item))); closeDetail() },
            () => closeDetail()
          )
        )
    }

    shell(
      spec,
      state,
      selectedKey = detailModeVar.signal.map(_.editing.map(spec.rowKey)),
      onRowClick = item => detailModeVar.set(DetailMode.Editing(item)),
      detail = detail,
      toolbarActions =
        List(button(cls := "btn btn-add", "+ Add", onClick --> (_ => detailModeVar.set(DetailMode.Creating))))
    )
  }

  /** Read-only counterpart to `apply`: same list behavior, but no "+ Add" button and no write actions. Clicking a row
    * shows `detailFields` (the table's columns by default) as static fields with just a Close button. Used by Flight
    * Instances and the `/view/...` preview pages.
    */
  def readOnly[T](spec: EntityListSpec[T], detailFields: List[(String, T => String)] = Nil): HtmlElement = {
    val state = ListState(spec)
    val fields = if (detailFields.isEmpty) spec.columns else detailFields
    val selectedVar = Var(Option.empty[T])

    val detail: Signal[Option[HtmlElement]] = selectedVar.signal.map(_.map { item =>
      div(
        cls := "detail-form",
        div(cls := "detail-heading", spec.title),
        fields.map { case (label, extract) => FormField.readOnly(label, extract(item)) },
        FormActions.close(() => selectedVar.set(None))
      )
    })

    shell(
      spec,
      state,
      selectedKey = selectedVar.signal.map(_.map(spec.rowKey)),
      onRowClick = item => selectedVar.set(Some(item)),
      detail = detail,
      toolbarActions = Nil
    )
  }
}
