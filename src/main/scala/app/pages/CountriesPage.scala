package app.pages

import app.api.CountriesApi
import app.api.Http.given
import app.components.AsyncAction
import app.components.EntityCrudPage
import app.components.FormActions
import app.components.FormField
import app.models.CountryDto
import app.models.CreateCountryRequest
import app.models.UpdateCountryRequest
import com.raquo.laminar.api.L._

object CountriesPage {

  private val sampleData = List(
    CountryDto("ES", "Spain"),
    CountryDto("FR", "France"),
    CountryDto("PT", "Portugal"),
    CountryDto("GB", "United Kingdom")
  )

  private def editForm(
      item: CountryDto,
      onSaved: CountryDto => Unit,
      onDeleted: () => Unit,
      onCancel: () => Unit
  ): HtmlElement = {
    val nameVar = Var(item.name)
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit =
      AsyncAction.run(savingVar, errVar)(CountriesApi.update(item.code, UpdateCountryRequest(nameVar.now().trim)))(
        onSaved
      )

    def remove(): Unit =
      AsyncAction.run(savingVar, errVar)(CountriesApi.delete(item.code))(_ => onDeleted())

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "Selected country"),
      FormField.readOnly("Code", item.code),
      FormField.text("Name", nameVar),
      FormField.errorBanner(errVar),
      FormActions.saveDeleteCancel(savingVar.signal, save, remove, onCancel)
    )
  }

  private def createForm(onCreated: CountryDto => Unit, onCancel: () => Unit): HtmlElement = {
    val codeVar = Var("")
    val nameVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit = {
      val req = CreateCountryRequest(codeVar.now().trim.toUpperCase, nameVar.now().trim)
      AsyncAction.run(savingVar, errVar)(CountriesApi.create(req))(onCreated)
    }

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "New country"),
      FormField.text("Code (ISO 3166-1 alpha-2)", codeVar, "ES"),
      FormField.text("Name", nameVar, "Spain"),
      FormField.errorBanner(errVar),
      FormActions.saveCancel(savingVar.signal, save, onCancel)
    )
  }

  // The backend's name filter requires at least this many characters; EntityCrudPage's minSearchLength keeps shorter
  // queries from ever reaching fetchPage at all (except when cleared back to empty).
  private val MinNameSearchLength = 3
  private def nameFilter(query: String): Option[String] = Option.when(query.trim.nonEmpty)(query.trim)

  private val columns: List[(String, CountryDto => String)] = List("Code" -> (_.code), "Name" -> (_.name))

  private val rowKey: CountryDto => String = _.code

  private def matchesSearch(c: CountryDto, needle: String): Boolean =
    c.name.toLowerCase.contains(needle) || c.code.toLowerCase.contains(needle)

  def apply(): HtmlElement =
    EntityCrudPage[CountryDto](
      title = "Countries",
      searchPlaceholder = "Search country by name (3+ characters)…",
      columns = columns,
      rowKey = rowKey,
      matchesSearch = matchesSearch,
      sampleData = sampleData,
      fetchPage = (page, query) => CountriesApi.list(name = nameFilter(query), page = page),
      renderCreateForm = createForm,
      renderEditForm = editForm,
      emptySelectionHint = "Select a country from the list, or click \"Add\".",
      minSearchLength = MinNameSearchLength,
      serverSearch = true
    )

  def readOnly(): HtmlElement =
    EntityCrudPage.readOnly[CountryDto](
      title = "Countries",
      searchPlaceholder = "Search country by name (3+ characters)…",
      columns = columns,
      rowKey = rowKey,
      matchesSearch = matchesSearch,
      sampleData = sampleData,
      fetchPage = (page, query) => CountriesApi.list(name = nameFilter(query), page = page),
      emptySelectionHint = "Select a country from the list to see its details. Viewer mode is read-only.",
      minSearchLength = MinNameSearchLength,
      serverSearch = true
    )
}
