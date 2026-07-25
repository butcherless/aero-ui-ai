package app.pages

import app.api.AircraftApi
import app.api.Http.given
import app.components.AsyncAction
import app.components.EntityCrudPage
import app.components.FormActions
import app.components.FormField
import app.models.AircraftDto
import app.models.CreateAircraftRequest
import app.models.UpdateAircraftRequest
import com.raquo.laminar.api.L._

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

  def apply(): HtmlElement =
    EntityCrudPage[AircraftDto](
      title = "Aircraft",
      searchPlaceholder = "Search aircraft (registration, type, airline)…",
      columns = List(
        "Registration" -> (_.registration),
        "Type" -> (_.typeCode),
        "Description" -> (_.description),
        "Airline" -> (_.airlineIcao)
      ),
      rowKey = _.registration,
      matchesSearch = (a, needle) =>
        a.registration.toLowerCase.contains(needle) ||
          a.typeCode.toLowerCase.contains(needle) ||
          a.description.toLowerCase.contains(needle) ||
          a.airlineIcao.toLowerCase.contains(needle),
      sampleData = sampleData,
      fetchPage = page => AircraftApi.list(page = page),
      renderCreateForm = createForm,
      renderEditForm = editForm,
      emptySelectionHint = "Select an aircraft from the list, or click \"Add\"."
    )
}
