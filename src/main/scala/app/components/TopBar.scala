package app.components

import app.api.AirlinesApi
import app.api.AirportsApi
import app.api.AuthApi
import app.api.CountriesApi
import app.api.Http.given
import app.auth.Session
import app.router.AppRouter
import app.router.Page
import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.util.Failure
import scala.util.Success

/** Top bar above the content area: brand logo and an exact-code quick-lookup widget on the left, the logged-in user's
  * name and a working Disconnect button on the right.
  */
object TopBar {

  // Self-contained inline SVG placeholder (generic user silhouette) — no network/asset dependency. Also reused by
  // ProfilePage so the two placeholder-identity spots in the app share one visual.
  val avatarPlaceholder =
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%239ca3af'%3E" +
      "%3Ccircle cx='12' cy='8' r='4'/%3E%3Cpath d='M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8'/%3E%3C/svg%3E"

  private sealed trait LookupKind
  private object LookupKind {
    case object Country extends LookupKind
    case object Airport extends LookupKind
    case object Airline extends LookupKind
  }

  /** Exact-key lookup (not the per-page name search): Country by ISO code, Airport by IATA, Airline by ICAO, each via
    * the backend's get-by-key endpoint. Shown as a small result card right under the widget rather than navigating
    * away, since it's meant as a quick "does this code exist, what is it" check from anywhere in the app.
    */
  private def quickLookup(): HtmlElement = {
    val kindVar = Var[LookupKind](LookupKind.Country)
    val codeVar = Var("")
    val resultVar = Var(Option.empty[Either[String, String]]) // Left = error message, Right = result summary

    def runLookup(): Unit = {
      val code = codeVar.now().trim
      if (code.isEmpty) resultVar.set(None)
      else {
        val summary = kindVar.now() match {
          case LookupKind.Country => CountriesApi.get(code.toUpperCase).map(c => s"${c.code} — ${c.name}")
          case LookupKind.Airport => AirportsApi.get(code.toUpperCase).map(a => s"${a.iata} — ${a.name}, ${a.city}")
          case LookupKind.Airline => AirlinesApi.get(code.toUpperCase).map(a => s"${a.icao} — ${a.name}")
        }
        summary.onComplete {
          case Success(text) => resultVar.set(Some(Right(text)))
          case Failure(_) => resultVar.set(Some(Left(s"No match for \"$code\".")))
        }
      }
    }

    div(
      cls := "quick-lookup",
      div(
        cls := "quick-lookup-row",
        select(
          cls := "quick-lookup-select",
          onChange.mapToValue --> Observer[String] { v =>
            kindVar.set(v match {
              case "airport" => LookupKind.Airport
              case "airline" => LookupKind.Airline
              case _ => LookupKind.Country
            })
            resultVar.set(None)
          },
          option(value := "country", "Country"),
          option(value := "airport", "Airport"),
          option(value := "airline", "Airline")
        ),
        input(
          cls := "quick-lookup-input",
          placeholder := "Code…",
          controlled(value <-- codeVar.signal, onInput.mapToValue --> codeVar.writer),
          onKeyDown.filter(_.key == "Enter") --> Observer[dom.KeyboardEvent](_ => runLookup())
        ),
        button(cls := "btn btn-secondary", "Go", onClick --> (_ => runLookup()))
      ),
      child.maybe <-- resultVar.signal.map(_.map {
        case Right(text) => div(cls := "quick-lookup-result", text)
        case Left(text) => div(cls := "quick-lookup-result quick-lookup-error", text)
      })
    )
  }

  // Best-effort server-side revocation — never blocks clearing the local session on it (the network
  // call can fail or hang; discarding the token client-side is what actually ends the session from
  // the UI's point of view, per the auth API contract).
  private def disconnect(): Unit = {
    AuthApi.logout().recover { case _ => () }
    Session.clear()
  }

  def apply(): HtmlElement =
    div(
      cls := "topbar",
      img(
        cls := "topbar-logo",
        src := "/martinair.png",
        alt := "MartinAir — go to home",
        title := "Home",
        onClick --> (_ => AppRouter.navigateTo(Page.Countries))
      ),
      quickLookup(),
      div(
        cls := "topbar-actions",
        img(cls := "topbar-avatar", src := avatarPlaceholder, alt := "User avatar"),
        span(cls := "topbar-username", text <-- Session.signal.map(_.map(_.username).getOrElse(""))),
        span(cls := "topbar-divider"),
        button(
          cls := "btn btn-secondary topbar-disconnect",
          "Disconnect",
          onClick --> (_ => disconnect())
        )
      )
    )
}
