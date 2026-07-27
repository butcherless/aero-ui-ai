package app.pages

import app.api.AuthApi
import app.api.Http
import app.api.Http.given
import app.auth.Session
import app.components.FormField
import app.models.LoginRequest
import app.router.AppRouter
import app.router.Page
import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.util.Failure
import scala.util.Success

/** Standalone login screen — not wrapped in `Layout`/`MasterDetailShell`, since it's shown instead of the sidebar/top
  * bar while unauthenticated (see `App.scala`'s auth-gated `appSignal`).
  */
object LoginPage {

  def apply(): HtmlElement = {
    val usernameVar = Var("")
    val passwordVar = Var("")
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])

    val canSubmit: Signal[Boolean] =
      usernameVar.signal.combineWith(passwordVar.signal).map { case (u, p) => u.trim.nonEmpty && p.nonEmpty }

    // Not AsyncAction.run: that always shows ex.getMessage, but login needs status-differentiated
    // copy (401 = wrong credentials, 400/other = generic) per the auth API contract.
    def submit(): Unit = {
      val username = usernameVar.now().trim
      savingVar.set(true)
      errVar.set(None)
      AuthApi.login(LoginRequest(username, passwordVar.now())).onComplete {
        case Success(response) =>
          savingVar.set(false)
          Session.store(username, response)
          // Deep-linking to a protected page falls out for free (App.scala's contentSignal keeps
          // pointing at whatever AppRouter.currentPageSignal already was) — but if the user landed
          // here by URL (or a session-expiry redirect) while sitting *on* /login itself, that path
          // never changes on its own, so the URL bar would otherwise stay stuck at /login even
          // though the authenticated view is now showing.
          if (AppRouter.pageVar.now() == Page.Login) AppRouter.navigateTo(Page.Countries)
        case Failure(Http.ApiError(401, _)) =>
          savingVar.set(false)
          errVar.set(Some("Incorrect username or password."))
        case Failure(Http.ApiError(400, rawText)) =>
          savingVar.set(false)
          dom.console.error(s"Login request rejected: $rawText")
          errVar.set(Some("Something went wrong, please try again."))
        case Failure(_) =>
          savingVar.set(false)
          errVar.set(Some("Something went wrong, please try again."))
      }
    }

    def trySubmit(): Unit =
      if (usernameVar.now().trim.nonEmpty && passwordVar.now().nonEmpty) submit()

    div(
      cls := "login-page",
      div(
        cls := "detail-form login-card",
        div(cls := "detail-heading", "Sign in"),
        FormField
          .text("Username", usernameVar, "admin")
          .amend(
            onKeyDown.filter(_.key == "Enter") --> Observer[dom.KeyboardEvent](_ => trySubmit())
          ),
        FormField
          .password("Password", passwordVar)
          .amend(
            onKeyDown.filter(_.key == "Enter") --> Observer[dom.KeyboardEvent](_ => trySubmit())
          ),
        FormField.errorBanner(errVar),
        div(
          cls := "detail-actions",
          button(
            cls := "btn btn-primary",
            "Sign in",
            disabled <-- savingVar.signal.combineWith(canSubmit).map { case (saving, ok) => saving || !ok },
            onClick --> (_ => submit())
          )
        )
      )
    )
  }
}
