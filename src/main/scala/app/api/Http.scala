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

  /** Shown by every entity page when the initial load fails and it falls back to sample data. */
  val backendUnreachableMessage: String =
    "Could not connect to the backend (http://localhost:8080). Showing sample data."

  given ExecutionContext = scala.concurrent.ExecutionContext.global

  /** Raised for any non-2xx response, carrying the backend's own error message when it sends one. */
  final case class ApiError(status: Int, message: String) extends RuntimeException(message)

  private def rawRequest(method: HttpMethod, path: String, body: Option[String]): Future[(Int, String)] = {
    val init = new RequestInit {}
    init.method = method
    init.headers = js.Dictionary("Content-Type" -> "application/json")
    body.foreach(b => init.body = b)

    dom.fetch(baseUrl + path, init).toFuture.flatMap { resp =>
      resp.text().toFuture.map(text => (resp.status, text))
    }
  }

  private def extractMessage(text: String): Option[String] =
    if (text.trim.isEmpty) None
    else Try(read[HttpErrorResponse](text).message).toOption

  private def call(method: HttpMethod, path: String, body: Option[String]): Future[String] =
    rawRequest(method, path, body).map {
      case (status, text) =>
        if (status >= 200 && status < 300) text
        else throw ApiError(status, extractMessage(text).getOrElse(s"Error HTTP $status"))
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
