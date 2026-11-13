package app.pages

import app.api.AirlinesApi
import app.api.Http
import app.api.Http.given
import app.components.AsyncAction
import app.components.EntityCrudPage
import app.components.EntityListSpec
import app.components.FormActions
import app.components.FormField
import app.components.FormValues.*
import app.models.AirlineDto
import app.models.CreateAirlineRequest
import app.models.UpdateAirlineRequest
import com.raquo.laminar.api.L.*

object AirlinesPage {

  private val sampleData = List(
    AirlineDto("IBE", "Iberia", alias = Some("Iberia Express"), callsign = Some("IBERIA"), iata = Some("IB")),
    AirlineDto("AEA", "Air Europa", callsign = Some("EUROPA"), iata = Some("UX")),
    AirlineDto("VLG", "Vueling", callsign = Some("VUELING"), iata = Some("V7"))
  )

  private val MissingCountryMessage =
    "Enter the airline's country code. The API doesn't return it, but requires it to save."

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
    // The backend never returns an airline's country (and, unlike airports, has no endpoint to look it up), but
    // requires it on update. So it starts blank and must be re-entered on every save.
    val countryCodeVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit =
      if (countryCodeVar.trimmed.isEmpty) errVar.set(Some(MissingCountryMessage))
      else {
        val req = UpdateAirlineRequest(
          nameVar.trimmed,
          countryCodeVar.code,
          aliasVar.optional,
          callsignVar.optional,
          iataVar.optionalCode
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
      FormField.text("Country (ISO code, required)", countryCodeVar, "Not returned by the API, re-enter (e.g. ES)"),
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
        icaoVar.code,
        nameVar.trimmed,
        countryCodeVar.code,
        aliasVar.optional,
        callsignVar.optional,
        iataVar.optionalCode
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

  private def listSpec(): EntityListSpec[AirlineDto] =
    CountryScopedList.listSpec(
      title = "Airlines",
      noun = "airline",
      columns = columns,
      rowKey = rowKey,
      matchesSearch = matchesSearch,
      sampleData = sampleData,
      endpoints = CountryScopedList.Endpoints(AirlinesApi.list(_), AirlinesApi.search, AirlinesApi.byCountry(_, _))
    )

  def apply(): HtmlElement = EntityCrudPage(listSpec(), createForm, editForm)

  def readOnly(): HtmlElement = EntityCrudPage.readOnly(listSpec())
}
