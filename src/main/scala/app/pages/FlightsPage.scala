package app.pages

import app.api.FlightsApi
import app.api.Http.given
import app.components.AsyncAction
import app.components.EntityCrudPage
import app.components.FormActions
import app.components.FormField
import app.models.CreateFlightRequest
import app.models.FlightDto
import app.models.UpdateFlightRequest
import com.raquo.laminar.api.L._

object FlightsPage {

  private val sampleData = List(
    FlightDto("UX9117", "07:05", "08:55", "MAD", "TFN", "AEA", alias = Some("AEA9117")),
    FlightDto("IB3170", "09:15", "10:35", "MAD", "BCN", "IBE"),
    FlightDto("VY1001", "12:00", "13:10", "BCN", "MAD", "VLG")
  )

  private def editForm(
      item: FlightDto,
      onSaved: FlightDto => Unit,
      onDeleted: () => Unit,
      onCancel: () => Unit
  ): HtmlElement = {
    val aliasVar = Var(item.alias.getOrElse(""))
    val schedDepartureVar = Var(item.schedDeparture)
    val schedArrivalVar = Var(item.schedArrival)
    val originIataVar = Var(item.originIata)
    val destinationIataVar = Var(item.destinationIata)
    val airlineIcaoVar = Var(item.airlineIcao)
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit = {
      val req = UpdateFlightRequest(
        schedDepartureVar.now().trim,
        schedArrivalVar.now().trim,
        originIataVar.now().trim.toUpperCase,
        destinationIataVar.now().trim.toUpperCase,
        airlineIcaoVar.now().trim.toUpperCase,
        Option(aliasVar.now().trim).filter(_.nonEmpty)
      )
      AsyncAction.run(savingVar, errVar)(FlightsApi.update(item.code, req))(onSaved)
    }

    def remove(): Unit =
      AsyncAction.run(savingVar, errVar)(FlightsApi.delete(item.code))(_ => onDeleted())

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "Selected flight"),
      FormField.readOnly("Code", item.code),
      FormField.text("Alias", aliasVar),
      FormField.text("Scheduled departure (HH:mm)", schedDepartureVar, "07:05"),
      FormField.text("Scheduled arrival (HH:mm)", schedArrivalVar, "08:55"),
      FormField.text("Origin (IATA)", originIataVar),
      FormField.text("Destination (IATA)", destinationIataVar),
      FormField.text("Airline (ICAO)", airlineIcaoVar),
      FormField.errorBanner(errVar),
      FormActions.saveDeleteCancel(savingVar.signal, save, remove, onCancel)
    )
  }

  private def createForm(onCreated: FlightDto => Unit, onCancel: () => Unit): HtmlElement = {
    val codeVar = Var("")
    val aliasVar = Var("")
    val schedDepartureVar = Var("")
    val schedArrivalVar = Var("")
    val originIataVar = Var("")
    val destinationIataVar = Var("")
    val airlineIcaoVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    def save(): Unit = {
      val req = CreateFlightRequest(
        codeVar.now().trim.toUpperCase,
        schedDepartureVar.now().trim,
        schedArrivalVar.now().trim,
        originIataVar.now().trim.toUpperCase,
        destinationIataVar.now().trim.toUpperCase,
        airlineIcaoVar.now().trim.toUpperCase,
        Option(aliasVar.now().trim).filter(_.nonEmpty)
      )
      AsyncAction.run(savingVar, errVar)(FlightsApi.create(req))(onCreated)
    }

    div(
      cls := "detail-form",
      div(cls := "detail-heading", "New flight"),
      FormField.text("Code", codeVar, "UX9117"),
      FormField.text("Alias", aliasVar, "AEA9117"),
      FormField.text("Scheduled departure (HH:mm)", schedDepartureVar, "07:05"),
      FormField.text("Scheduled arrival (HH:mm)", schedArrivalVar, "08:55"),
      FormField.text("Origin (IATA)", originIataVar, "MAD"),
      FormField.text("Destination (IATA)", destinationIataVar, "TFN"),
      FormField.text("Airline (ICAO)", airlineIcaoVar, "AEA"),
      FormField.errorBanner(errVar),
      FormActions.saveCancel(savingVar.signal, save, onCancel)
    )
  }

  def apply(): HtmlElement =
    EntityCrudPage[FlightDto](
      title = "Flights",
      searchPlaceholder = "Search flight (code, origin, destination, airline)…",
      columns = List(
        "Code" -> (_.code),
        "Origin" -> (_.originIata),
        "Destination" -> (_.destinationIata),
        "Departure" -> (_.schedDeparture),
        "Arrival" -> (_.schedArrival),
        "Airline" -> (_.airlineIcao)
      ),
      rowKey = _.code,
      matchesSearch = (f, needle) =>
        f.code.toLowerCase.contains(needle) ||
          f.originIata.toLowerCase.contains(needle) ||
          f.destinationIata.toLowerCase.contains(needle) ||
          f.airlineIcao.toLowerCase.contains(needle),
      sampleData = sampleData,
      fetchAll = () => FlightsApi.list(),
      renderCreateForm = createForm,
      renderEditForm = editForm,
      emptySelectionHint = "Select a flight from the list, or click \"Add\"."
    )
}
