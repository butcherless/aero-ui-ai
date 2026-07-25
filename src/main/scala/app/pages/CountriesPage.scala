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

  def apply(): HtmlElement =
    EntityCrudPage[CountryDto](
      title = "Countries",
      searchPlaceholder = "Search country (name or code)…",
      columns = List("Code" -> (_.code), "Name" -> (_.name)),
      rowKey = _.code,
      matchesSearch = (c, needle) => c.name.toLowerCase.contains(needle) || c.code.toLowerCase.contains(needle),
      sampleData = sampleData,
      fetchPage = page => CountriesApi.list(page = page),
      renderCreateForm = createForm,
      renderEditForm = editForm,
      emptySelectionHint = "Select a country from the list, or click \"Add\"."
    )
}
