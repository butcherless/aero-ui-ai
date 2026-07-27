package app.auth

import app.models.LoginResponse
import com.raquo.laminar.api.L._
import org.scalajs.dom
import upickle.default.ReadWriter
import upickle.default.read
import upickle.default.write

import scala.util.Try

case class SessionData(token: String, tokenType: String, username: String, expiresAt: Long) derives ReadWriter

/** Client-side auth session: the token, when it expires, and who's logged in. Persisted to `localStorage` so a page
  * refresh doesn't force a fresh login before the token's natural expiry.
  */
object Session {

  private val storageKey = "aero-ui-session"

  // package-private (not fully private) so SessionSpec can exercise the expiry-discarding logic
  // directly — Session is a module-level singleton, so its one real init-time load can't otherwise
  // be re-triggered mid-test-run to observe this behavior.
  private[auth] def loadFromStorage(): Option[SessionData] =
    Option(dom.window.localStorage.getItem(storageKey))
      .flatMap(json => Try(read[SessionData](json)).toOption)
      .filter(_.expiresAt > System.currentTimeMillis())

  val sessionVar: Var[Option[SessionData]] = Var(loadFromStorage())
  val signal: Signal[Option[SessionData]] = sessionVar.signal
  val isAuthenticated: Signal[Boolean] = signal.map(_.isDefined)

  def token: Option[String] = sessionVar.now().map(_.token)

  def store(username: String, response: LoginResponse): Unit = {
    val data = SessionData(
      response.token,
      response.tokenType,
      username,
      System.currentTimeMillis() + response.expiresIn.toLong * 1000
    )
    sessionVar.set(Some(data))
    dom.window.localStorage.setItem(storageKey, write(data))
  }

  def clear(): Unit = {
    sessionVar.set(None)
    dom.window.localStorage.removeItem(storageKey)
  }
}
