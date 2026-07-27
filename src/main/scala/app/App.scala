package app

import app.components.Layout
import app.pages._
import app.router.AppRouter
import app.router.Page
import com.raquo.laminar.api.L._
import org.scalajs.dom

object App {

  def main(args: Array[String]): Unit = {
    val contentSignal: Signal[HtmlElement] = AppRouter.currentPageSignal.map {
      case Page.Countries => CountriesPage()
      case Page.Airports => AirportsPage()
      case Page.Airlines => AirlinesPage()
      case Page.Aircraft => AircraftPage()
      case Page.Flights => FlightsPage()
      case Page.FlightInstances => FlightInstancesPage()
      case Page.Routes => RoutesPage()
      case Page.Profile => ProfilePage()
      case Page.ViewCountries => CountriesPage.readOnly()
      case Page.ViewAirports => AirportsPage.readOnly()
      case Page.ViewAirlines => AirlinesPage.readOnly()
      case Page.ViewAircraft => AircraftPage.readOnly()
      case Page.ViewFlights => FlightsPage.readOnly()
      case Page.ViewRoutes => RoutesPage.readOnly()
    }

    val containerNode = dom.document.getElementById("app-container")
    render(containerNode, Layout(contentSignal))
  }
}
