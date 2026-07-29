package app.pages

import app.api.AircraftApi
import app.api.Http.given
import app.components.AsyncAction
import app.components.DebouncedFilterInput
import app.components.EntityCrudPage
import app.components.FormActions
import app.components.FormField
import app.models.AircraftDto
import app.models.CreateAircraftRequest
import app.models.UpdateAircraftRequest
import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.annotation.unused
import scala.concurrent.Future

object AircraftPage {

  private val sampleData = List(
    AircraftDto("EC-MIG", "B788", "Boeing 787-8 Dreamliner", "IBE"),
    AircraftDto("EC-NBA", "A339", "Airbus A330-900", "IBE"),
    AircraftDto("EC-MTA", "A320", "Airbus A320", "VLG")
  )

  private def editForm(
      item: AircraftDto,
      onSaved: AircraftDto => Unit,
      onDeleted: () => Unit,
      onCancel: () => Unit
  ): HtmlElement = {
    val typeCodeVar = Var(item.typeCode)
    val descriptionVar = Var(item.description)
    val airlineIcaoVar = Var(item.airlineIcao)
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit = {
      val req =
        UpdateAircraftRequest(typeCodeVar.now().trim, descriptionVar.now().trim, airlineIcaoVar.now().trim.toUpperCase)
      AsyncAction.run(savingVar, errVar)(AircraftApi.update(item.registration, req))(onSaved)
    }

    def remove(): Unit =
      AsyncAction.run(savingVar, errVar)(AircraftApi.delete(item.registration))(_ => onDeleted())

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "Selected aircraft"),
      FormField.readOnly("Registration", item.registration),
      FormField.text("Type (ICAO)", typeCodeVar),
      FormField.text("Description", descriptionVar),
      FormField.text("Airline (ICAO)", airlineIcaoVar),
      FormField.errorBanner(errVar),
      FormActions.saveDeleteCancel(savingVar.signal, save, remove, onCancel)
    )
  }

  private def createForm(onCreated: AircraftDto => Unit, onCancel: () => Unit): HtmlElement = {
    val registrationVar = Var("")
    val typeCodeVar = Var("")
    val descriptionVar = Var("")
    val airlineIcaoVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit = {
      val req = CreateAircraftRequest(
        registrationVar.now().trim.toUpperCase,
        typeCodeVar.now().trim,
        descriptionVar.now().trim,
        airlineIcaoVar.now().trim.toUpperCase
      )
      AsyncAction.run(savingVar, errVar)(AircraftApi.create(req))(onCreated)
    }

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "New aircraft"),
      FormField.text("Registration", registrationVar, "EC-MIG"),
      FormField.text("Type (ICAO)", typeCodeVar, "B788"),
      FormField.text("Description", descriptionVar, "Boeing 787-8 Dreamliner"),
      FormField.text("Airline (ICAO)", airlineIcaoVar, "IBE"),
      FormField.errorBanner(errVar),
      FormActions.saveCancel(savingVar.signal, save, onCancel)
    )
  }

  // Unlike Airports/Airlines, Aircraft has no partial-search endpoint and no query param on the list
  // endpoint — only an exact-match GET .../aircraft/{registration}. So the registration box fires on an
  // explicit button/Enter rather than a debounced character threshold, and always performs an exact lookup.
  // A 404 (or any failure) is treated as "no results", not the sample-data fallback error banner.
  private def fetchAircraft(
      registrationVar: Var[String],
      airlineFilterVar: Var[String]
  )(page: Int, @unused query: String): Future[List[AircraftDto]] = {
    val registration = registrationVar.now().trim.toUpperCase
    val airline = airlineFilterVar.now().trim.toUpperCase
    if (registration.nonEmpty) AircraftApi.get(registration).map(List(_)).recover { case _ => Nil }
    else if (airline.nonEmpty) AircraftApi.byAirline(airline, page)
    else AircraftApi.list(page = page)
  }

  // ICAO airline codes are exactly 3 letters, so 3 is both the minimum and the practical maximum useful
  // length — debounced auto-fire mirrors Airports/Airlines' country filter.
  private val MinAirlineSearchLength = 3

  // The two boxes and the built-in search box are mutually exclusive (focusing one clears the others),
  // same rationale as Airports/Airlines: the backend has no endpoint combining registration + airline.
  private def extraToolbar(
      registrationVar: Var[String],
      airlineFilterVar: Var[String]
  )(reload: () => Unit, clearSearch: () => Unit): List[HtmlElement] = {
    def submitRegistration(): Unit = if (registrationVar.now().trim.nonEmpty) reload()

    List(
      input(
        cls := "search-input",
        placeholder := "Registration (exact, e.g. EC-MIG)",
        controlled(value <-- registrationVar.signal, onInput.mapToValue --> registrationVar.writer),
        onFocus --> (_ => { clearSearch(); airlineFilterVar.set("") }),
        onKeyDown.filter(_.key == "Enter") --> Observer[dom.KeyboardEvent](_ => submitRegistration())
      ),
      button(cls := "btn btn-secondary", "Search", onClick --> (_ => submitRegistration())),
      DebouncedFilterInput(
        airlineFilterVar,
        "Airline ICAO code (e.g. IBE)",
        MinAirlineSearchLength,
        reload,
        () => { clearSearch(); registrationVar.set("") }
      )
    )
  }

  private val columns: List[(String, AircraftDto => String)] = List(
    "Registration" -> (_.registration),
    "Type" -> (_.typeCode),
    "Description" -> (_.description),
    "Airline" -> (_.airlineIcao)
  )

  private val rowKey: AircraftDto => String = _.registration

  private def matchesSearch(a: AircraftDto, needle: String): Boolean =
    a.registration.toLowerCase.contains(needle) ||
      a.typeCode.toLowerCase.contains(needle) ||
      a.description.toLowerCase.contains(needle) ||
      a.airlineIcao.toLowerCase.contains(needle)

  def apply(): HtmlElement = {
    val registrationVar = Var("")
    val airlineFilterVar = Var("")
    EntityCrudPage[AircraftDto](
      title = "Aircraft",
      searchPlaceholder = "Search aircraft (registration, type, airline)…",
      columns = columns,
      rowKey = rowKey,
      matchesSearch = matchesSearch,
      sampleData = sampleData,
      fetchPage = fetchAircraft(registrationVar, airlineFilterVar),
      renderCreateForm = createForm,
      renderEditForm = editForm,
      emptySelectionHint = "Select an aircraft from the list, or click \"Add\".",
      renderExtraToolbar = extraToolbar(registrationVar, airlineFilterVar),
      onSearchFocus = () => { registrationVar.set(""); airlineFilterVar.set("") }
    )
  }

  def readOnly(): HtmlElement = {
    val registrationVar = Var("")
    val airlineFilterVar = Var("")
    EntityCrudPage.readOnly[AircraftDto](
      title = "Aircraft",
      searchPlaceholder = "Search aircraft (registration, type, airline)…",
      columns = columns,
      rowKey = rowKey,
      matchesSearch = matchesSearch,
      sampleData = sampleData,
      fetchPage = fetchAircraft(registrationVar, airlineFilterVar),
      emptySelectionHint = "Select an aircraft from the list to see its details. Viewer mode is read-only.",
      renderExtraToolbar = extraToolbar(registrationVar, airlineFilterVar),
      onSearchFocus = () => { registrationVar.set(""); airlineFilterVar.set("") }
    )
  }
}
