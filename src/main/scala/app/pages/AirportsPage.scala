package app.pages

import app.api.AirportsApi
import app.api.Http
import app.api.Http.given
import app.components.AsyncAction
import app.components.EntityCrudPage
import app.components.FormActions
import app.components.FormField
import app.models.AirportDto
import app.models.CreateAirportRequest
import app.models.UpdateAirportRequest
import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

object AirportsPage {

  private val sampleData = List(
    AirportDto("MAD", "LEMD", "Adolfo Suárez Madrid-Barajas", "Madrid"),
    AirportDto("BCN", "LEBL", "Barcelona-El Prat", "Barcelona"),
    AirportDto("LHR", "EGLL", "London Heathrow", "London"),
    AirportDto("CDG", "LFPG", "Paris Charles de Gaulle", "Paris")
  )

  private def editForm(
      item: AirportDto,
      onSaved: AirportDto => Unit,
      onDeleted: () => Unit,
      onCancel: () => Unit
  ): HtmlElement = {
    val icaoVar = Var(item.icaoCode)
    val nameVar = Var(item.name)
    val cityVar = Var(item.city)
    val countryCodeVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    // The list endpoint doesn't return the country, so look it up separately to prefill the field.
    AirportsApi.countryOf(item.iata).onComplete {
      case Success(country) => countryCodeVar.set(country.code)
      case Failure(_) => ()
    }

    def save(): Unit = {
      val req = UpdateAirportRequest(
        icaoVar.now().trim,
        nameVar.now().trim,
        cityVar.now().trim,
        countryCodeVar.now().trim.toUpperCase
      )
      AsyncAction.run(savingVar, errVar)(AirportsApi.update(item.iata, req))(onSaved)
    }

    def remove(): Unit =
      AsyncAction.run(savingVar, errVar)(AirportsApi.delete(item.iata))(_ => onDeleted())

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "Selected airport"),
      FormField.readOnly("IATA", item.iata),
      FormField.text("ICAO", icaoVar),
      FormField.text("Name", nameVar),
      FormField.text("City", cityVar),
      FormField.text("Country (ISO code)", countryCodeVar, "ES"),
      FormField.errorBanner(errVar),
      FormActions.saveDeleteCancel(savingVar.signal, save, remove, onCancel)
    )
  }

  private def createForm(onCreated: AirportDto => Unit, onCancel: () => Unit): HtmlElement = {
    val iataVar = Var("")
    val icaoVar = Var("")
    val nameVar = Var("")
    val cityVar = Var("")
    val countryCodeVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit = {
      val req = CreateAirportRequest(
        iataVar.now().trim.toUpperCase,
        icaoVar.now().trim.toUpperCase,
        nameVar.now().trim,
        cityVar.now().trim,
        countryCodeVar.now().trim.toUpperCase
      )
      AsyncAction.run(savingVar, errVar)(AirportsApi.create(req))(onCreated)
    }

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "New airport"),
      FormField.text("IATA", iataVar, "MAD"),
      FormField.text("ICAO", icaoVar, "LEMD"),
      FormField.text("Name", nameVar, "Adolfo Suárez Madrid-Barajas"),
      FormField.text("City", cityVar, "Madrid"),
      FormField.text("Country (ISO code)", countryCodeVar, "ES"),
      FormField.errorBanner(errVar),
      FormActions.saveCancel(savingVar.signal, save, onCancel)
    )
  }

  // The backend's airport search (/airports/search) requires 3+ characters and returns every match unpaginated, so
  // fetchPage slices that full match list itself to keep Prev/Next behaving the same way it does for a plain list.
  private val MinNameSearchLength = 3

  // ISO 3166-1 alpha-2 codes are exactly 2 letters, so 2 is both the minimum and the practical maximum useful length.
  private val MinCountrySearchLength = 2

  private def fetchAirports(countryFilterVar: Var[String])(page: Int, query: String): Future[List[AirportDto]] = {
    val country = countryFilterVar.now().trim.toUpperCase
    if (country.nonEmpty) AirportsApi.byCountry(country, page)
    else if (query.trim.length >= MinNameSearchLength)
      AirportsApi.search(query.trim).map { matches =>
        val pageSize = Http.defaultPageSize
        matches.slice((page - 1) * pageSize, page * pageSize)
      }
    else AirportsApi.list(page = page)
  }

  // Country-by-code browse. Takes priority over the name search when set, since the backend has no endpoint
  // combining both — so focusing either box clears the other to avoid the confusing state of both holding text at
  // once. Debounced auto-fire at 2+ characters mirrors the name search box's own serverSearch behavior.
  private def countryFilterControl(
      filterVar: Var[String],
      reload: () => Unit,
      clearSearch: () => Unit
  ): List[HtmlElement] =
    List(
      input(
        cls := "search-input",
        placeholder := "Country code (e.g. ES)",
        controlled(value <-- filterVar.signal, onInput.mapToValue --> filterVar.writer),
        onFocus --> (_ => clearSearch()),
        onInput(_.debounce(350).map(_.target.asInstanceOf[dom.html.Input].value)) -->
          Observer[String] { v =>
            val trimmed = v.trim
            if (trimmed.isEmpty || trimmed.length >= MinCountrySearchLength) reload()
          }
      )
    )

  def apply(): HtmlElement = {
    val countryFilterVar = Var("")
    EntityCrudPage[AirportDto](
      title = "Airports",
      searchPlaceholder = "Search airport by name (3+ characters)…",
      columns = List("IATA" -> (_.iata), "ICAO" -> (_.icaoCode), "Name" -> (_.name), "City" -> (_.city)),
      rowKey = _.iata,
      matchesSearch = (a, needle) =>
        a.name.toLowerCase.contains(needle) ||
          a.city.toLowerCase.contains(needle) ||
          a.iata.toLowerCase.contains(needle) ||
          a.icaoCode.toLowerCase.contains(needle),
      sampleData = sampleData,
      fetchPage = fetchAirports(countryFilterVar),
      renderCreateForm = createForm,
      renderEditForm = editForm,
      emptySelectionHint = "Select an airport from the list, or click \"Add\".",
      serverSearch = true,
      minSearchLength = MinNameSearchLength,
      renderExtraToolbar = (reload, clearSearch) => countryFilterControl(countryFilterVar, reload, clearSearch),
      onSearchFocus = () => countryFilterVar.set("")
    )
  }

  def readOnly(): HtmlElement = {
    val countryFilterVar = Var("")
    EntityCrudPage.readOnly[AirportDto](
      title = "Airports",
      searchPlaceholder = "Search airport by name (3+ characters)…",
      columns = List("IATA" -> (_.iata), "ICAO" -> (_.icaoCode), "Name" -> (_.name), "City" -> (_.city)),
      rowKey = _.iata,
      matchesSearch = (a, needle) =>
        a.name.toLowerCase.contains(needle) ||
          a.city.toLowerCase.contains(needle) ||
          a.iata.toLowerCase.contains(needle) ||
          a.icaoCode.toLowerCase.contains(needle),
      sampleData = sampleData,
      fetchPage = fetchAirports(countryFilterVar),
      emptySelectionHint = "Select an airport from the list to see its details. Viewer mode is read-only.",
      serverSearch = true,
      minSearchLength = MinNameSearchLength,
      renderExtraToolbar = (reload, clearSearch) => countryFilterControl(countryFilterVar, reload, clearSearch),
      onSearchFocus = () => countryFilterVar.set("")
    )
  }
}
