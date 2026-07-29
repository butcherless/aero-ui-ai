package app.pages

import app.api.Http
import app.api.Http.given
import app.api.RoutesApi
import app.components.AsyncAction
import app.components.EntityTable
import app.components.FormActions
import app.components.FormField
import app.components.MasterDetailShell
import app.models.AirlineDto
import app.models.CreateRouteRequest
import app.models.RouteDto
import com.raquo.laminar.api.L._

import scala.util.Failure
import scala.util.Success

/** Routes have no list-all/get-by-key/update/delete endpoints, so this page browses routes by the airline operating
  * them, and manages airline<->route associations in the detail panel.
  */
object RoutesPage {

  private val sampleData = List(
    RouteDto("MAD", "TFN", 1740),
    RouteDto("MAD", "BCN", 483)
  )

  private def rowKey(r: RouteDto): String = s"${r.originIata}-${r.destinationIata}"

  private val InvalidIcaoMessage = "Enter a 3-letter airline ICAO code (e.g. AEA)."
  private val SearchPlaceholder = "View an airline's routes (ICAO, e.g. AEA)…"

  // The browse-by-airline-ICAO search state is identical for the editable and read-only variants — they only differ
  // in what the detail panel lets you do with a selected route, not in how the list itself is searched/loaded.
  private final case class BrowseState(
      itemsVar: Var[List[RouteDto]],
      loadingVar: Var[Boolean],
      errorVar: Var[Option[String]],
      queryIcaoVar: Var[String],
      search: () => Unit
  )

  private def buildBrowseState(): BrowseState = {
    val itemsVar = Var(List.empty[RouteDto])
    val loadingVar = Var(false)
    val errorVar = Var(Option.empty[String])
    val queryIcaoVar = Var("")

    def search(): Unit = {
      val icao = queryIcaoVar.now().trim.toUpperCase
      if (icao.length != 3) {
        errorVar.set(Some(InvalidIcaoMessage))
      } else {
        loadingVar.set(true)
        errorVar.set(None)
        RoutesApi.byAirline(icao).onComplete {
          case Success(list) =>
            loadingVar.set(false)
            itemsVar.set(list)
          case Failure(ex) =>
            val failure = Http.loadFailure(ex)
            loadingVar.set(false)
            errorVar.set(Some(failure.message))
            itemsVar.set(if (failure.useSampleData) sampleData else Nil)
        }
      }
    }

    BrowseState(itemsVar, loadingVar, errorVar, queryIcaoVar, search)
  }

  // `editable` toggles the associate-a-new-airline input and the per-chip ✕ remove button; the read-only variant
  // shows the same chip list with neither.
  private def airlinesSubsection(route: RouteDto, editable: Boolean): HtmlElement = {
    val airlinesVar = Var(List.empty[AirlineDto])
    val newIcaoVar = Var("")
    val subErrVar = Var(Option.empty[String])

    def reload(): Unit =
      RoutesApi.airlinesOperating(route.originIata, route.destinationIata).onComplete {
        case Success(list) => airlinesVar.set(list)
        case Failure(_) => airlinesVar.set(Nil)
      }
    reload()

    def add(): Unit = {
      val icao = newIcaoVar.now().trim.toUpperCase
      if (icao.length == 3) {
        RoutesApi.associateAirline(route.originIata, route.destinationIata, icao).onComplete {
          case Success(_) =>
            newIcaoVar.set("")
            subErrVar.set(None)
            reload()
          case Failure(ex) => subErrVar.set(Some(ex.getMessage))
        }
      }
    }

    def remove(icao: String): Unit =
      RoutesApi.disassociateAirline(route.originIata, route.destinationIata, icao).onComplete {
        case Success(_) => reload()
        case Failure(ex) => subErrVar.set(Some(ex.getMessage))
      }

    def chip(a: AirlineDto): HtmlElement =
      if (editable) div(cls := "chip", a.icao, button("✕", onClick --> (_ => remove(a.icao))))
      else div(cls := "chip", a.icao)

    val writeControls: List[HtmlElement] =
      if (!editable) Nil
      else
        List(
          div(
            cls := "entity-toolbar",
            input(
              cls := "search-input",
              placeholder := "Airline ICAO (e.g. AEA)",
              controlled(value <-- newIcaoVar.signal, onInput.mapToValue --> newIcaoVar.writer)
            ),
            button(cls := "btn btn-secondary", "Associate", onClick --> (_ => add()))
          ),
          FormField.errorBanner(subErrVar)
        )

    div(
      cls := "detail-subsection",
      div(cls := "detail-subsection-title", "Airlines operating this route"),
      div(cls := "chip-list", children <-- airlinesVar.signal.map(_.map(chip))),
      writeControls
    )
  }

  private def detailView(route: RouteDto, editable: Boolean, onClose: () => Unit): HtmlElement =
    div(
      cls := "detail-form",
      div(cls := "detail-heading", "Selected route"),
      FormField.readOnly("Origin", route.originIata),
      FormField.readOnly("Destination", route.destinationIata),
      FormField.readOnly("Distance (km)", route.distanceKm.toString),
      FormActions.close(onClose),
      airlinesSubsection(route, editable)
    )

  private def routeTable(
      rows: Signal[List[RouteDto]],
      selectedKey: Signal[Option[String]],
      onRowClick: RouteDto => Unit,
      loading: Signal[Boolean],
      error: Signal[Option[String]]
  ): HtmlElement =
    EntityTable[RouteDto](
      columns = List(
        "Origin" -> (_.originIata),
        "Destination" -> (_.destinationIata),
        "Distance (km)" -> (_.distanceKm.toString)
      ),
      rows = rows,
      rowKey = rowKey,
      selectedKey = selectedKey,
      onRowClick = onRowClick,
      loading = loading,
      error = error
    )

  private sealed trait DetailMode
  private case object NoSelection extends DetailMode
  private case object Creating extends DetailMode
  private case class Editing(item: RouteDto) extends DetailMode

  private def createForm(itemsVar: Var[List[RouteDto]], detailModeVar: Var[DetailMode]): HtmlElement = {
    val originVar = Var("")
    val destinationVar = Var("")
    val distanceVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit =
      distanceVar.now().trim.toIntOption match {
        case None =>
          errVar.set(Some("Distance must be a whole number of kilometers."))
        case Some(km) =>
          val req = CreateRouteRequest(originVar.now().trim.toUpperCase, destinationVar.now().trim.toUpperCase, km)
          AsyncAction.run(savingVar, errVar)(RoutesApi.create(req)) { created =>
            itemsVar.update(_ :+ created)
            detailModeVar.set(Editing(created))
          }
      }

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "New route"),
      FormField.text("Origin (IATA)", originVar, "MAD"),
      FormField.text("Destination (IATA)", destinationVar, "TFN"),
      FormField.number("Distance (km)", distanceVar, "1740"),
      FormField.errorBanner(errVar),
      FormActions.saveCancel(savingVar.signal, save, () => detailModeVar.set(NoSelection))
    )
  }

  def apply(): HtmlElement = {
    val state = buildBrowseState()
    import state._

    val detailModeVar = Var[DetailMode](NoSelection)

    val toolbar = div(
      cls := "entity-toolbar",
      input(
        cls := "search-input",
        placeholder := SearchPlaceholder,
        controlled(value <-- queryIcaoVar.signal, onInput.mapToValue --> queryIcaoVar.writer)
      ),
      button(cls := "btn btn-secondary", "Search", onClick --> (_ => search())),
      button(cls := "btn btn-add", "+ Add", onClick --> (_ => detailModeVar.set(Creating)))
    )

    val list = routeTable(
      rows = itemsVar.signal,
      selectedKey = detailModeVar.signal.map { case Editing(r) => Some(rowKey(r)); case _ => None },
      onRowClick = item => detailModeVar.set(Editing(item)),
      loading = loadingVar.signal,
      error = errorVar.signal
    )

    val detail: Signal[Option[HtmlElement]] = detailModeVar.signal.map {
      case NoSelection => None
      case Creating => Some(createForm(itemsVar, detailModeVar))
      case Editing(item) => Some(detailView(item, editable = true, onClose = () => detailModeVar.set(NoSelection)))
    }

    MasterDetailShell("Routes", toolbar, list, detail)
  }

  /** Read-only counterpart to `apply`: same browse-by-airline search, no "+ Add" button, and the airlines subsection is
    * shown without the associate/disassociate controls.
    */
  def readOnly(): HtmlElement = {
    val state = buildBrowseState()
    import state._

    val selectedVar = Var(Option.empty[RouteDto])

    val toolbar = div(
      cls := "entity-toolbar",
      input(
        cls := "search-input",
        placeholder := SearchPlaceholder,
        controlled(value <-- queryIcaoVar.signal, onInput.mapToValue --> queryIcaoVar.writer)
      ),
      button(cls := "btn btn-secondary", "Search", onClick --> (_ => search()))
    )

    val list = routeTable(
      rows = itemsVar.signal,
      selectedKey = selectedVar.signal.map(_.map(rowKey)),
      onRowClick = item => selectedVar.set(Some(item)),
      loading = loadingVar.signal,
      error = errorVar.signal
    )

    val detail: Signal[Option[HtmlElement]] = selectedVar.signal.map {
      case None => None
      case Some(item) => Some(detailView(item, editable = false, onClose = () => selectedVar.set(None)))
    }

    MasterDetailShell("Routes", toolbar, list, detail)
  }
}
