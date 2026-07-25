package app.pages

import app.api.AirlinesApi
import app.api.Http.given
import app.components.AsyncAction
import app.components.EntityCrudPage
import app.components.FormActions
import app.components.FormField
import app.models.AirlineDto
import app.models.CreateAirlineRequest
import app.models.UpdateAirlineRequest
import com.raquo.laminar.api.L._

object AirlinesPage {

  private val sampleData = List(
    AirlineDto("IBE", "Iberia", alias = Some("Iberia Express"), callsign = Some("IBERIA")),
    AirlineDto("AEA", "Air Europa", callsign = Some("EUROPA")),
    AirlineDto("VLG", "Vueling", callsign = Some("VUELING"))
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
    val countryCodeVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit = {
      val req = UpdateAirlineRequest(
        nameVar.now().trim,
        countryCodeVar.now().trim.toUpperCase,
        Option(aliasVar.now().trim).filter(_.nonEmpty),
        Option(callsignVar.now().trim).filter(_.nonEmpty)
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
    val countryCodeVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit = {
      val req = CreateAirlineRequest(
        icaoVar.now().trim.toUpperCase,
        nameVar.now().trim,
        countryCodeVar.now().trim.toUpperCase,
        Option(aliasVar.now().trim).filter(_.nonEmpty),
        Option(callsignVar.now().trim).filter(_.nonEmpty)
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
      FormField.text("Country (ISO code)", countryCodeVar, "ES"),
      FormField.errorBanner(errVar),
      FormActions.saveCancel(savingVar.signal, save, onCancel)
    )
  }

  def apply(): HtmlElement =
    EntityCrudPage[AirlineDto](
      title = "Airlines",
      searchPlaceholder = "Search airline (name, ICAO, alias)…",
      columns = List(
        "ICAO" -> (_.icao),
        "Name" -> (_.name),
        "Alias" -> (a => a.alias.getOrElse("—")),
        "Callsign" -> (a => a.callsign.getOrElse("—"))
      ),
      rowKey = _.icao,
      matchesSearch = (a, needle) =>
        a.name.toLowerCase.contains(needle) ||
          a.icao.toLowerCase.contains(needle) ||
          a.alias.exists(_.toLowerCase.contains(needle)),
      sampleData = sampleData,
      fetchPage = page => AirlinesApi.list(page = page),
      renderCreateForm = createForm,
      renderEditForm = editForm,
      emptySelectionHint = "Select an airline from the list, or click \"Add\"."
    )
}
