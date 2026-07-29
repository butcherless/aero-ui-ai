package app.pages

import app.api.AirlinesApi
import app.api.Http
import app.api.Http.given
import app.components.AsyncAction
import app.components.DebouncedFilterInput
import app.components.EntityCrudPage
import app.components.FormActions
import app.components.FormField
import app.models.AirlineDto
import app.models.CreateAirlineRequest
import app.models.UpdateAirlineRequest
import com.raquo.laminar.api.L._

import scala.concurrent.Future

object AirlinesPage {

  private val sampleData = List(
    AirlineDto("IBE", "Iberia", alias = Some("Iberia Express"), callsign = Some("IBERIA"), iata = Some("IB")),
    AirlineDto("AEA", "Air Europa", callsign = Some("EUROPA"), iata = Some("UX")),
    AirlineDto("VLG", "Vueling", callsign = Some("VUELING"), iata = Some("V7"))
  )

  private def editForm(
      item: AirlineDto,
      onSaved: AirlineDto => Unit,
      onDeleted: () => Unit,
      onCancel: () => Unit
  ): HtmlElement = {
    val nameVar = Var(item.name)
    val aliasVar = Var(item.alias.getOrElse(""))
    val callsignVar = Var(item.callsign.getOrElse(""))
    val iataVar = Var(item.iata.getOrElse(""))
    val countryCodeVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit = {
      val req = UpdateAirlineRequest(
        nameVar.now().trim,
        countryCodeVar.now().trim.toUpperCase,
        Option(aliasVar.now().trim).filter(_.nonEmpty),
        Option(callsignVar.now().trim).filter(_.nonEmpty),
        Option(iataVar.now().trim.toUpperCase).filter(_.nonEmpty)
      )
      AsyncAction.run(savingVar, errVar)(AirlinesApi.update(item.icao, req))(onSaved)
    }

    def remove(): Unit =
      AsyncAction.run(savingVar, errVar)(AirlinesApi.delete(item.icao))(_ => onDeleted())

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "Selected airline"),
      FormField.readOnly("ICAO", item.icao),
      FormField.text("Name", nameVar),
      FormField.text("Alias", aliasVar),
      FormField.text("Callsign", callsignVar),
      FormField.text("IATA", iataVar, "IB"),
      FormField.text("Country (ISO code)", countryCodeVar, "ES"),
      FormField.errorBanner(errVar),
      FormActions.saveDeleteCancel(savingVar.signal, save, remove, onCancel)
    )
  }

  private def createForm(onCreated: AirlineDto => Unit, onCancel: () => Unit): HtmlElement = {
    val icaoVar = Var("")
    val nameVar = Var("")
    val aliasVar = Var("")
    val callsignVar = Var("")
    val iataVar = Var("")
    val countryCodeVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit = {
      val req = CreateAirlineRequest(
        icaoVar.now().trim.toUpperCase,
        nameVar.now().trim,
        countryCodeVar.now().trim.toUpperCase,
        Option(aliasVar.now().trim).filter(_.nonEmpty),
        Option(callsignVar.now().trim).filter(_.nonEmpty),
        Option(iataVar.now().trim.toUpperCase).filter(_.nonEmpty)
      )
      AsyncAction.run(savingVar, errVar)(AirlinesApi.create(req))(onCreated)
    }

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "New airline"),
      FormField.text("ICAO", icaoVar, "IBE"),
      FormField.text("Name", nameVar, "Iberia"),
      FormField.text("Alias", aliasVar, "Iberia Express"),
      FormField.text("Callsign", callsignVar, "IBERIA"),
      FormField.text("IATA", iataVar, "IB"),
      FormField.text("Country (ISO code)", countryCodeVar, "ES"),
      FormField.errorBanner(errVar),
      FormActions.saveCancel(savingVar.signal, save, onCancel)
    )
  }

  // The backend's airline search (/airlines/search) requires 3+ characters and returns every match unpaginated, so
  // fetchPage slices that full match list itself to keep Prev/Next behaving the same way it does for a plain list.
  private val MinNameSearchLength = 3

  // ISO 3166-1 alpha-2 codes are exactly 2 letters, so 2 is both the minimum and the practical maximum useful length.
  private val MinCountrySearchLength = 2

  private def fetchAirlines(countryFilterVar: Var[String])(page: Int, query: String): Future[List[AirlineDto]] = {
    val country = countryFilterVar.now().trim.toUpperCase
    if (country.nonEmpty) AirlinesApi.byCountry(country, page)
    else if (query.trim.length >= MinNameSearchLength)
      AirlinesApi.search(query.trim).map { matches =>
        val pageSize = Http.defaultPageSize
        matches.slice((page - 1) * pageSize, page * pageSize)
      }
    else AirlinesApi.list(page = page)
  }

  // Country-by-code browse. Takes priority over the name search when set, since the backend has no endpoint
  // combining both — so focusing either box clears the other to avoid the confusing state of both holding text at
  // once. Debounced auto-fire at 2+ characters mirrors the name search box's own serverSearch behavior.
  private def countryFilterControl(
      filterVar: Var[String],
      reload: () => Unit,
      clearSearch: () => Unit
  ): List[HtmlElement] =
    List(DebouncedFilterInput(filterVar, "Country code (e.g. ES)", MinCountrySearchLength, reload, clearSearch))

  private val columns: List[(String, AirlineDto => String)] = List(
    "ICAO" -> (_.icao),
    "Name" -> (_.name),
    "Alias" -> (a => a.alias.getOrElse("—")),
    "Callsign" -> (a => a.callsign.getOrElse("—")),
    "IATA" -> (a => a.iata.getOrElse("—"))
  )

  private val rowKey: AirlineDto => String = _.icao

  private def matchesSearch(a: AirlineDto, needle: String): Boolean =
    a.name.toLowerCase.contains(needle) ||
      a.icao.toLowerCase.contains(needle) ||
      a.alias.exists(_.toLowerCase.contains(needle)) ||
      a.iata.exists(_.toLowerCase.contains(needle))

  def apply(): HtmlElement = {
    val countryFilterVar = Var("")
    EntityCrudPage[AirlineDto](
      title = "Airlines",
      searchPlaceholder = "Search airline by name (3+ characters)…",
      columns = columns,
      rowKey = rowKey,
      matchesSearch = matchesSearch,
      sampleData = sampleData,
      fetchPage = fetchAirlines(countryFilterVar),
      renderCreateForm = createForm,
      renderEditForm = editForm,
      emptySelectionHint = "Select an airline from the list, or click \"Add\".",
      serverSearch = true,
      minSearchLength = MinNameSearchLength,
      renderExtraToolbar = (reload, clearSearch) => countryFilterControl(countryFilterVar, reload, clearSearch),
      onSearchFocus = () => countryFilterVar.set("")
    )
  }

  def readOnly(): HtmlElement = {
    val countryFilterVar = Var("")
    EntityCrudPage.readOnly[AirlineDto](
      title = "Airlines",
      searchPlaceholder = "Search airline by name (3+ characters)…",
      columns = columns,
      rowKey = rowKey,
      matchesSearch = matchesSearch,
      sampleData = sampleData,
      fetchPage = fetchAirlines(countryFilterVar),
      emptySelectionHint = "Select an airline from the list to see its details. Viewer mode is read-only.",
      serverSearch = true,
      minSearchLength = MinNameSearchLength,
      renderExtraToolbar = (reload, clearSearch) => countryFilterControl(countryFilterVar, reload, clearSearch),
      onSearchFocus = () => countryFilterVar.set("")
    )
  }
}
