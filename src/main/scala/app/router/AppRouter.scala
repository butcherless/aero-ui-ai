package app.router

import com.raquo.laminar.api.L._
import org.scalajs.dom

/** The set of pages the dashboard can show. Add a case here for every new page. */
sealed trait Page
object Page {
  case object Countries extends Page
  case object Airports extends Page
  case object Airlines extends Page
  case object Aircraft extends Page
  case object Flights extends Page
  case object FlightInstances extends Page
  case object Routes extends Page

  def fromPath(path: String): Page = path match {
    case "/airports" => Airports
    case "/airlines" => Airlines
    case "/aircraft" => Aircraft
    case "/flights" => Flights
    case "/flight-instances" => FlightInstances
    case "/routes" => Routes
    case _ => Countries
  }

  def toPath(page: Page): String = page match {
    case Countries => "/"
    case Airports => "/airports"
    case Airlines => "/airlines"
    case Aircraft => "/aircraft"
    case Flights => "/flights"
    case FlightInstances => "/flight-instances"
    case Routes => "/routes"
  }
}

/** Minimal client-side router built directly on the browser History API.
  *
  * This is intentionally dependency-free so the skeleton compiles with just Laminar + scalajs-dom. Swap it out for
  * [[https://github.com/raquo/Waypoint Waypoint]] once you need route params, query strings, etc.
  */
object AppRouter {
  import Page._

  val pageVar: Var[Page] = Var(fromPath(dom.window.location.pathname))
  val currentPageSignal: Signal[Page] = pageVar.signal

  // Keep state in sync with the browser's back/forward buttons
  dom.window.addEventListener(
    "popstate",
    (_: dom.PopStateEvent) => pageVar.set(fromPath(dom.window.location.pathname))
  )

  def navigateTo(page: Page): Unit = {
    val path = toPath(page)
    if (dom.window.location.pathname != path) {
      dom.window.history.pushState(null, "", path)
    }
    pageVar.set(page)
  }
}
