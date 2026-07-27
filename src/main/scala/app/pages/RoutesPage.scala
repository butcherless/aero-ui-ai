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

  private sealed trait DetailMode
  private case object NoSelection extends DetailMode
  private case object Creating extends DetailMode
  private case class Editing(item: RouteDto) extends DetailMode

  private val itemsVar = Var(List.empty[RouteDto])
  private val loadingVar = Var(false)
  private val errorVar = Var(Option.empty[String])
  private val queryIcaoVar = Var("")
  private val detailModeVar = Var[DetailMode](NoSelection)

  private def rowKey(r: RouteDto): String = s"${r.originIata}-${r.destinationIata}"

  private def search(): Unit = {
    val icao = queryIcaoVar.now().trim.toUpperCase
    if (icao.length != 3) {
      errorVar.set(Some("Enter a 3-letter airline ICAO code (e.g. AEA)."))
    } else {
      loadingVar.set(true)
      errorVar.set(None)
      RoutesApi.byAirline(icao).onComplete {
        case Success(list) =>
          loadingVar.set(false)
          itemsVar.set(list)
        case Failure(_) =>
          loadingVar.set(false)
          errorVar.set(Some(Http.backendUnreachableMessage))
          itemsVar.set(sampleData)
      }
    }
  }

  private def airlinesSubsection(route: RouteDto): HtmlElement = {
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

    div(
      cls := "detail-subsection",
      div(cls := "detail-subsection-title", "Airlines operating this route"),
      div(
        cls := "chip-list",
        children <-- airlinesVar.signal.map(
          _.map(a => div(cls := "chip", a.icao, button("✕", onClick --> (_ => remove(a.icao)))))
        )
      ),
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
  }

  private def editView(route: RouteDto): HtmlElement =
    div(
      cls := "detail-form",
      div(cls := "detail-heading", "Selected route"),
      FormField.readOnly("Origin", route.originIata),
      FormField.readOnly("Destination", route.destinationIata),
      FormField.readOnly("Distance (km)", route.distanceKm.toString),
      FormActions.close(() => detailModeVar.set(NoSelection)),
      airlinesSubsection(route)
    )

  private def createForm(): HtmlElement = {
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
    val toolbar = div(
      cls := "entity-toolbar",
      input(
        cls := "search-input",
        placeholder := "View an airline's routes (ICAO, e.g. AEA)…",
        controlled(value <-- queryIcaoVar.signal, onInput.mapToValue --> queryIcaoVar.writer)
      ),
      button(cls := "btn btn-secondary", "Search", onClick --> (_ => search())),
      button(cls := "btn btn-add", "+ Add", onClick --> (_ => detailModeVar.set(Creating)))
    )

    val list = EntityTable[RouteDto](
      columns = List(
        "Origin" -> (_.originIata),
        "Destination" -> (_.destinationIata),
        "Distance (km)" -> (_.distanceKm.toString)
      ),
      rows = itemsVar.signal,
      rowKey = rowKey,
      selectedKey = detailModeVar.signal.map { case Editing(r) => Some(rowKey(r)); case _ => None },
      onRowClick = item => detailModeVar.set(Editing(item)),
      loading = loadingVar.signal,
      error = errorVar.signal
    )

    val detail: Signal[HtmlElement] = detailModeVar.signal.map {
      case NoSelection =>
        div(cls := "detail-placeholder", "Search for an airline's routes, select a row, or click \"Add\".")
      case Creating => createForm()
      case Editing(item) => editView(item)
    }

    MasterDetailShell("Routes", toolbar, list, detail)
  }

  private val roItemsVar = Var(List.empty[RouteDto])
  private val roLoadingVar = Var(false)
  private val roErrorVar = Var(Option.empty[String])
  private val roQueryIcaoVar = Var("")
  private val roSelectedVar = Var(Option.empty[RouteDto])

  private def roSearch(): Unit = {
    val icao = roQueryIcaoVar.now().trim.toUpperCase
    if (icao.length != 3) {
      roErrorVar.set(Some("Enter a 3-letter airline ICAO code (e.g. AEA)."))
    } else {
      roLoadingVar.set(true)
      roErrorVar.set(None)
      RoutesApi.byAirline(icao).onComplete {
        case Success(list) =>
          roLoadingVar.set(false)
          roItemsVar.set(list)
        case Failure(_) =>
          roLoadingVar.set(false)
          roErrorVar.set(Some(Http.backendUnreachableMessage))
          roItemsVar.set(sampleData)
      }
    }
  }

  private def roAirlinesSubsection(route: RouteDto): HtmlElement = {
    val airlinesVar = Var(List.empty[AirlineDto])

    RoutesApi.airlinesOperating(route.originIata, route.destinationIata).onComplete {
      case Success(list) => airlinesVar.set(list)
      case Failure(_) => airlinesVar.set(Nil)
    }

    div(
      cls := "detail-subsection",
      div(cls := "detail-subsection-title", "Airlines operating this route"),
      div(
        cls := "chip-list",
        children <-- airlinesVar.signal.map(_.map(a => div(cls := "chip", a.icao)))
      )
    )
  }

  private def roDetailView(route: RouteDto): HtmlElement =
    div(
      cls := "detail-form",
      div(cls := "detail-heading", "Selected route"),
      FormField.readOnly("Origin", route.originIata),
      FormField.readOnly("Destination", route.destinationIata),
      FormField.readOnly("Distance (km)", route.distanceKm.toString),
      FormActions.close(() => roSelectedVar.set(None)),
      roAirlinesSubsection(route)
    )

  /** Read-only counterpart to `apply`: same browse-by-airline search, no "+ Add" button, and the airlines subsection is
    * shown without the associate/disassociate controls. Uses its own module-level state, separate from `apply`'s, so
    * the two pages don't interfere with each other's in-progress search/selection.
    */
  def readOnly(): HtmlElement = {
    val toolbar = div(
      cls := "entity-toolbar",
      input(
        cls := "search-input",
        placeholder := "View an airline's routes (ICAO, e.g. AEA)…",
        controlled(value <-- roQueryIcaoVar.signal, onInput.mapToValue --> roQueryIcaoVar.writer)
      ),
      button(cls := "btn btn-secondary", "Search", onClick --> (_ => roSearch()))
    )

    val list = EntityTable[RouteDto](
      columns = List(
        "Origin" -> (_.originIata),
        "Destination" -> (_.destinationIata),
        "Distance (km)" -> (_.distanceKm.toString)
      ),
      rows = roItemsVar.signal,
      rowKey = rowKey,
      selectedKey = roSelectedVar.signal.map(_.map(rowKey)),
      onRowClick = item => roSelectedVar.set(Some(item)),
      loading = roLoadingVar.signal,
      error = roErrorVar.signal
    )

    val detail: Signal[HtmlElement] = roSelectedVar.signal.map {
      case None =>
        div(cls := "detail-placeholder", "Search for an airline's routes, then select a row. Viewer mode is read-only.")
      case Some(item) => roDetailView(item)
    }

    MasterDetailShell("Routes", toolbar, list, detail)
  }
}
