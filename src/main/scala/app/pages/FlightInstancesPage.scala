package app.pages

import app.api.FlightInstancesApi
import app.api.Http
import app.api.Http.given
import app.components.EntityTable
import app.components.FormActions
import app.components.FormField
import app.components.MasterDetailShell
import app.components.Pagination
import app.models.FlightInstanceDto
import com.raquo.laminar.api.L._

import scala.util.Failure
import scala.util.Success

/** Flight instances have no create/update/delete endpoints in the API — this page is view-only. */
object FlightInstancesPage {

  private val sampleData = List(
    FlightInstanceDto(
      "b1c2d3e4-f5a6-7890-bcde-f01234567890",
      "2024-06-28T15:23:00",
      "2024-06-28T19:41:00",
      "UX9117",
      "EC-MIG"
    ),
    FlightInstanceDto(
      "a1b2c3d4-e5f6-7890-abcd-ef0123456789",
      "2024-06-29T09:15:00",
      "2024-06-29T10:35:00",
      "IB3170",
      "EC-NBA"
    )
  )

  private val itemsVar = Var(List.empty[FlightInstanceDto])
  private val loadingVar = Var(true)
  private val errorVar = Var(Option.empty[String])
  private val searchVar = Var("")
  private val selectedVar = Var(Option.empty[FlightInstanceDto])
  private val pageVar = Var(1)
  private val hasNextVar = Var(false)

  private def load(page: Int): Unit = {
    loadingVar.set(true)
    errorVar.set(None)
    FlightInstancesApi.list(page = page).onComplete {
      case Success(list) =>
        loadingVar.set(false)
        pageVar.set(page)
        hasNextVar.set(list.size >= Http.defaultPageSize)
        itemsVar.set(list)
      case Failure(ex) =>
        val failure = Http.loadFailure(ex)
        loadingVar.set(false)
        errorVar.set(Some(failure.message))
        pageVar.set(page)
        hasNextVar.set(false)
        itemsVar.set(if (failure.useSampleData) sampleData else Nil)
    }
  }

  private def goToPrevPage(): Unit = if (pageVar.now() > 1) load(pageVar.now() - 1)
  private def goToNextPage(): Unit = if (hasNextVar.now()) load(pageVar.now() + 1)

  private def filtered: Signal[List[FlightInstanceDto]] =
    itemsVar.signal.combineWith(searchVar.signal).map {
      case (items, q) =>
        val needle = q.trim.toLowerCase
        if (needle.isEmpty) items
        else
          items.filter(fi =>
            fi.flightCode.toLowerCase.contains(needle) ||
              fi.registration.toLowerCase.contains(needle) ||
              fi.id.toLowerCase.contains(needle)
          )
    }

  private def detailView(item: FlightInstanceDto): HtmlElement =
    div(
      cls := "detail-form",
      div(cls := "detail-heading", "Flight instance"),
      FormField.readOnly("ID", item.id),
      FormField.readOnly("Flight", item.flightCode),
      FormField.readOnly("Aircraft", item.registration),
      FormField.readOnly("Actual departure", item.departureDate),
      FormField.readOnly("Actual arrival", item.arrivalDate),
      FormActions.close(() => selectedVar.set(None))
    )

  def apply(): HtmlElement = {
    val toolbar = div(
      cls := "entity-toolbar",
      input(
        cls := "search-input",
        placeholder := "Search instance (flight, registration, id)…",
        controlled(value <-- searchVar.signal, onInput.mapToValue --> searchVar.writer)
      )
    )

    val list = EntityTable[FlightInstanceDto](
      columns = List(
        "Flight" -> (_.flightCode),
        "Aircraft" -> (_.registration),
        "Actual departure" -> (_.departureDate),
        "Actual arrival" -> (_.arrivalDate)
      ),
      rows = filtered,
      rowKey = _.id,
      selectedKey = selectedVar.signal.map(_.map(_.id)),
      onRowClick = item => selectedVar.set(Some(item)),
      loading = loadingVar.signal,
      error = errorVar.signal
    )

    val pagination = Pagination(pageVar.signal, hasNextVar.signal, goToPrevPage, goToNextPage)

    val detail: Signal[Option[HtmlElement]] = selectedVar.signal.map {
      case None => None
      case Some(item) => Some(detailView(item))
    }

    MasterDetailShell("Flight Instances", toolbar, div(list, pagination), detail).amend(onMountCallback(_ => load(1)))
  }
}
