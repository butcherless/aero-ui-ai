package app.pages

import app.api.FlightInstancesApi
import app.components.EntityCrudPage
import app.models.FlightInstanceDto
import com.raquo.laminar.api.L.*

/** Flight instances have no create/update/delete endpoints in the API — this page is view-only. */
object FlightInstancesPage {

  private val sampleData = List(
    FlightInstanceDto(
      "b1c2d3e4-f5a6-7890-bcde-f01234567890",
      "2024-06-28T15:23:00",
      "2024-06-28T19:41:00",
      "UX9117",
      "EC-MIG"
    ),
    FlightInstanceDto(
      "a1b2c3d4-e5f6-7890-abcd-ef0123456789",
      "2024-06-29T09:15:00",
      "2024-06-29T10:35:00",
      "IB3170",
      "EC-NBA"
    )
  )

  private val columns: List[(String, FlightInstanceDto => String)] = List(
    "Flight" -> (_.flightCode),
    "Aircraft" -> (_.registration),
    "Actual departure" -> (_.departureDate),
    "Actual arrival" -> (_.arrivalDate)
  )

  private def matchesSearch(fi: FlightInstanceDto, needle: String): Boolean =
    fi.flightCode.toLowerCase.contains(needle) ||
      fi.registration.toLowerCase.contains(needle) ||
      fi.id.toLowerCase.contains(needle)

  def apply(): HtmlElement =
    EntityCrudPage.readOnly[FlightInstanceDto](
      title = "Flight Instances",
      searchPlaceholder = "Search instance (flight, registration, id)…",
      columns = columns,
      rowKey = _.id,
      matchesSearch = matchesSearch,
      sampleData = sampleData,
      fetchPage = (page, _) => FlightInstancesApi.list(page = page),
      detailFields = ("ID" -> ((fi: FlightInstanceDto) => fi.id)) :: columns
    )
}
