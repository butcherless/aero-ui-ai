package app.api

import app.models.HttpErrorResponse
import org.scalajs.dom
import org.scalajs.dom.HttpMethod
import org.scalajs.dom.RequestInit
import upickle.default.Reader
import upickle.default.Writer
import upickle.default.read
import upickle.default.write

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.js
import scala.util.Try

/** Thin wrapper around the Fetch API talking to the aviation backend described in openapi.yaml. */
object Http {

  // Relative, not absolute: the backend has no CORS support, so requests go through the Vite
  // dev-server proxy (see vite.config.js) to stay same-origin from the browser's point of view.
  val baseUrl = ""

  /** Default page size for list endpoints (the backend accepts 1-100, defaulting to 20 itself). */
  val defaultPageSize = 20

  /** Shown by every entity page's initial-load failure banner when the backend genuinely couldn't be reached at all
    * (DNS/connection failure, no `fetch` global, etc.) — as opposed to `loadFailure`'s `ApiError` case, where the
    * backend answered, just not with what was asked for.
    */
  val backendUnreachableMessage: String =
    "Could not connect to the backend (http://localhost:8080). Showing sample data."

  given ExecutionContext = scala.concurrent.ExecutionContext.global

  /** Raised for any non-2xx response, carrying the backend's own error message when it sends one. */
  final case class ApiError(status: Int, message: String) extends RuntimeException(message)

  /** What an entity page's initial-load failure banner should show, and whether it's meaningful to fall back to sample
    * data. `useSampleData` is the key distinction: a real HTTP error response (ApiError) means the backend is up and is
    * truthfully telling us there's nothing to show for this request (e.g. "Country not found: AA") — substituting
    * unrelated sample rows there would misrepresent an empty result as data. Only a genuine network failure, where the
    * backend can't be reached at all, warrants the sample-data fallback that keeps the app demoable.
    */
  final case class LoadFailure(message: String, useSampleData: Boolean)

  def loadFailure(ex: Throwable): LoadFailure = ex match {
    case ApiError(_, message) => LoadFailure(message, useSampleData = false)
    case _ => LoadFailure(backendUnreachableMessage, useSampleData = true)
  }

  // Wrapped in Future(...) so a synchronous throw from dom.fetch itself (e.g. no `fetch` global at
  // all, as under plain jsdom) becomes a failed Future like every other error case, instead of an
  // uncaught exception escaping straight out of whatever click handler triggered the call.
  //
  // Session.token is read synchronously, *before* entering the Future block — callers like
  // TopBar's disconnect (which fires the logout call then immediately clears the session on the
  // very next line) need the token captured at call time, not whenever the deferred fetch actually
  // runs, or the request would go out with no Authorization header at all.
  private def rawRequest(method: HttpMethod, path: String, body: Option[String]): Future[(Int, String)] = {
    val headers = js.Dictionary("Content-Type" -> "application/json")
    app.auth.Session.token.foreach(t => headers("Authorization") = s"Bearer $t")

    Future {
      val init = new RequestInit {}
      init.method = method
      init.headers = headers
      body.foreach(b => init.body = b)
      dom.fetch(baseUrl + path, init)
    }.flatMap(_.toFuture).flatMap(resp => resp.text().toFuture.map(text => (resp.status, text)))
  }

  // Falls back to the raw body when it isn't the {message, errors} JSON shape — every endpoint's
  // Tapir-level validation errors (bad path/query param, missing header) come back as plain text,
  // not JSON, and that text is worth keeping rather than discarding into a generic "Error HTTP $status".
  private def extractMessage(text: String): Option[String] =
    if (text.trim.isEmpty) None
    else Try(read[HttpErrorResponse](text).message).toOption.orElse(Some(text.trim))

  private val loginPath = "/api/v1/auth/login"

  private def call(method: HttpMethod, path: String, body: Option[String]): Future[String] =
    rawRequest(method, path, body).map {
      case (status, text) =>
        if (status >= 200 && status < 300) text
        else {
          // A 401 from anywhere except login itself means the session is no longer valid (missing,
          // expired, or invalid token) — clear it and bounce to login from this one central place,
          // rather than every call site handling it. Login's own 401 is a "wrong password" event,
          // not a "session died" event, and must surface as an inline form error instead.
          if (status == 401 && path != loginPath) {
            app.auth.Session.clear()
            app.router.AppRouter.navigateTo(app.router.Page.Login)
          }
          throw ApiError(status, extractMessage(text).getOrElse(s"Error HTTP $status"))
        }
    }

  def get(path: String): Future[String] = call(HttpMethod.GET, path, None)
  def post(path: String, body: String): Future[String] = call(HttpMethod.POST, path, Some(body))
  def postEmpty(path: String): Future[String] = call(HttpMethod.POST, path, None)
  def put(path: String, body: String): Future[String] = call(HttpMethod.PUT, path, Some(body))
  def delete(path: String): Future[String] = call(HttpMethod.DELETE, path, None)

  def getJson[T: Reader](path: String): Future[T] = get(path).map(read[T](_))
  def getJsonList[T: Reader](path: String): Future[List[T]] = get(path).map(read[List[T]](_))
  def postJson[Req: Writer, Res: Reader](path: String, body: Req): Future[Res] =
    post(path, write(body)).map(read[Res](_))
  def putJson[Req: Writer, Res: Reader](path: String, body: Req): Future[Res] = put(path, write(body)).map(read[Res](_))
  def deleteReq(path: String): Future[Unit] = delete(path).map(_ => ())
  def postEmptyReq(path: String): Future[Unit] = postEmpty(path).map(_ => ())

  /** Builds a `?k=v&k2=v2` query string, dropping empty/absent values. */
  def query(params: (String, Option[String])*): String = {
    val pairs = params.collect {
      case (k, Some(v)) if v.nonEmpty =>
        s"$k=${js.URIUtils.encodeURIComponent(v)}"
    }
    if (pairs.isEmpty) "" else "?" + pairs.mkString("&")
  }
}
