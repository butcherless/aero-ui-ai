package app.components

import app.api.Http
import app.api.Http.given
import com.raquo.laminar.api.L._

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
  * Aircraft, Flights): list loading with sample-data fallback, client-side search filtering, row selection, and wiring
  * into MasterDetailShell. Each entity supplies only its API calls and its create/edit form bodies, since the fields
  * differ per entity.
  */
object EntityCrudPage {

  def apply[T](
      title: String,
      searchPlaceholder: String,
      columns: List[(String, T => String)],
      rowKey: T => String,
      matchesSearch: (T, String) => Boolean,
      sampleData: List[T],
      fetchAll: () => Future[List[T]],
      renderCreateForm: (T => Unit, () => Unit) => HtmlElement,
      renderEditForm: (T, T => Unit, () => Unit, () => Unit) => HtmlElement,
      emptySelectionHint: String
  ): HtmlElement = {

    val itemsVar = Var(List.empty[T])
    val loadingVar = Var(true)
    val errorVar = Var(Option.empty[String])
    val searchVar = Var("")
    val detailModeVar = Var[DetailMode[T]](DetailMode.NoSelection)

    def load(): Unit = {
      loadingVar.set(true)
      errorVar.set(None)
      fetchAll().onComplete {
        case Success(list) =>
          loadingVar.set(false)
          itemsVar.set(list)
        case Failure(_) =>
          loadingVar.set(false)
          errorVar.set(Some(Http.backendUnreachableMessage))
          itemsVar.set(sampleData)
      }
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
        controlled(value <-- searchVar.signal, onInput.mapToValue --> searchVar.writer)
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

    MasterDetailShell(title, toolbar, list, detail).amend(onMountCallback(_ => load()))
  }
}
