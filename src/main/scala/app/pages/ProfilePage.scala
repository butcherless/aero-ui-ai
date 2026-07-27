package app.pages

import app.auth.Session
import app.components.TopBar
import app.router.AppRouter
import app.router.Page
import com.raquo.laminar.api.L._

/** Profile page: shows the logged-in user's identity, and links out to the read-only "Viewer" preview of every entity,
  * since the backend's roles/permissions module hasn't been implemented — anyone with a valid token sees everything, so
  * this lets the read-only UI be exercised ahead of real role-based access.
  */
object ProfilePage {

  private case class PreviewLink(icon: String, label: String, page: Page)

  private val previewLinks = List(
    PreviewLink("🌐", "Countries", Page.ViewCountries),
    PreviewLink("📍", "Airports", Page.ViewAirports),
    PreviewLink("✈️", "Airlines", Page.ViewAirlines),
    PreviewLink("🛩️", "Aircraft", Page.ViewAircraft),
    PreviewLink("🛫", "Flights", Page.ViewFlights),
    PreviewLink("🔀", "Routes", Page.ViewRoutes),
    PreviewLink("🕒", "Flight Instances (already read-only)", Page.FlightInstances)
  )

  def apply(): HtmlElement =
    div(
      cls := "page",
      h1("Profile"),
      div(
        cls := "profile-identity",
        img(cls := "topbar-avatar", src := TopBar.avatarPlaceholder, alt := "User avatar"),
        span(cls := "topbar-username", text <-- Session.signal.map(_.map(_.username).getOrElse("Admin User")))
      ),
      div(
        cls := "banner banner-info",
        "Role-based access isn't implemented on the backend yet. Until it is, use the links below to preview what " +
          "a restricted \"Viewer\" role would see: the same entities and data, but read-only."
      ),
      div(cls := "sidebar-section-label", "Read-only preview"),
      div(
        cls := "profile-preview-links",
        previewLinks.map { link =>
          button(
            cls := "btn btn-secondary",
            link.icon,
            " ",
            link.label,
            onClick --> (_ => AppRouter.navigateTo(link.page))
          )
        }
      )
    )
}
