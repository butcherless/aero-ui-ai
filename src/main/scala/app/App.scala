package app

import app.auth.Session
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
      // Only reachable if an already-logged-in user manually visits /login; bounce to the home page
      // instead of showing a blank/broken state (mirrors Page.fromPath's own unknown-path fallback).
      case Page.Login => CountriesPage()
    }

    // Hard-gated on auth: only reconstructs Layout/LoginPage on an actual login/logout transition
    // (isAuthenticated flipping), not on every page nav — Layout's own `child <-- content` keeps
    // handling page-to-page swaps exactly as before.
    val appSignal: Signal[HtmlElement] = Session.isAuthenticated.map {
      case true => Layout(contentSignal)
      case false => LoginPage()
    }

    val containerNode = dom.document.getElementById("app-container")
    render(containerNode, div(child <-- appSignal))
  }
}
