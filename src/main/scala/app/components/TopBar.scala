package app.components

import com.raquo.laminar.api.L._

/** Static top bar above the content area: brand logo on the left, and placeholder user/session UI on the right (no auth
  * system exists yet, so the avatar and "Disconnect" button aren't wired to anything real).
  */
object TopBar {

  // Self-contained inline SVG placeholder (generic user silhouette) — no network/asset dependency.
  private val avatarPlaceholder =
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%239ca3af'%3E" +
      "%3Ccircle cx='12' cy='8' r='4'/%3E%3Cpath d='M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8'/%3E%3C/svg%3E"

  def apply(): HtmlElement =
    div(
      cls := "topbar",
      img(cls := "topbar-logo", src := "/martinair.png", alt := "MartinAir"),
      div(
        cls := "topbar-actions",
        img(cls := "topbar-avatar", src := avatarPlaceholder, alt := "User avatar"),
        span(cls := "topbar-username", "Admin User"),
        span(cls := "topbar-divider"),
        button(cls := "btn btn-secondary topbar-disconnect", disabled := true, "Disconnect")
      )
    )
}
