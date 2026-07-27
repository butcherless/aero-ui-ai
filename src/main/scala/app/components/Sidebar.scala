package app.components

import app.router.AppRouter
import app.router.Page
import com.raquo.laminar.api.L._

object Sidebar {

  def apply(): HtmlElement =
    div(
      cls := "sidebar",
      h2("Admin Panel"),
      div(cls := "sidebar-section-label", "Entities"),
      navLink("🌐", "Countries", Page.Countries),
      navLink("📍", "Airports", Page.Airports),
      navLink("✈️", "Airlines", Page.Airlines),
      navLink("🛩️", "Aircraft", Page.Aircraft),
      navLink("🛫", "Flights", Page.Flights),
      navLink("🕒", "Flight Instances", Page.FlightInstances),
      navLink("🔀", "Routes", Page.Routes),
      div(cls := "sidebar-section-label", "User"),
      navLink("👤", "Profile", Page.Profile)
    )

  private def navLink(icon: String, label: String, page: Page): HtmlElement =
    button(
      cls <-- AppRouter.currentPageSignal.map(current => if (current == page) "nav-link active" else "nav-link"),
      span(cls := "nav-icon", icon),
      label,
      onClick --> (_ => AppRouter.navigateTo(page))
    )
}
