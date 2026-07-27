package app.api

import app.models._

import scala.concurrent.Future

object AuthApi {

  def login(req: LoginRequest): Future[LoginResponse] =
    Http.postJson[LoginRequest, LoginResponse]("/api/v1/auth/login", req)

  /** Revokes the current token server-side. Best-effort by design — see `TopBar`'s Disconnect handler, which never
    * waits on this before clearing the local session.
    */
  def logout(): Future[Unit] =
    Http.postEmptyReq("/api/v1/auth/logout")
}
