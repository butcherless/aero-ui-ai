package app.router

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** The set of pages the dashboard can show, each with its URL path. Add a case here for every new page. */
enum Page(val path: String) {
  case Countries extends Page("/")
  case Airports extends Page("/airports")
  case Airlines extends Page("/airlines")
  case Aircraft extends Page("/aircraft")
  case Flights extends Page("/flights")
  case FlightInstances extends Page("/flight-instances")
  case Routes extends Page("/routes")
  case Profile extends Page("/profile")
  case ViewCountries extends Page("/view/countries")
  case ViewAirports extends Page("/view/airports")
  case ViewAirlines extends Page("/view/airlines")
  case ViewAircraft extends Page("/view/aircraft")
  case ViewFlights extends Page("/view/flights")
  case ViewRoutes extends Page("/view/routes")
  case Login extends Page("/login")
}

object Page {

  /** The page at `path`, or Countries for an unknown one. Derived from each case's `path`, so the mapping is written
    * once.
    */
  def fromPath(path: String): Page = values.find(_.path == path).getOrElse(Countries)

  def toPath(page: Page): String = page.path
}

/** Minimal client-side router built directly on the browser History API.
  *
  * This is intentionally dependency-free so the skeleton compiles with just Laminar + scalajs-dom. Swap it out for
  * [[https://github.com/raquo/Waypoint Waypoint]] once you need route params, query strings, etc.
  */
object AppRouter {
  import Page.*

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
