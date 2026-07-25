package app.components

import com.raquo.laminar.api.L._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

/** Runs a Future while tracking a "saving" flag and surfacing failures into an error Var — the shape every
  * create/save/delete handler in the entity pages follows: flip `saving` on, clear the previous error, run the call,
  * and on completion flip `saving` off and either hand the result to `onSuccess` or show the failure message.
  */
object AsyncAction {

  def run[T](savingVar: Var[Boolean], errVar: Var[Option[String]])(action: => Future[T])(onSuccess: T => Unit)(using
      ExecutionContext
  ): Unit = {
    savingVar.set(true)
    errVar.set(None)
    action.onComplete {
      case Success(value) =>
        savingVar.set(false)
        onSuccess(value)
      case Failure(ex) =>
        savingVar.set(false)
        errVar.set(Some(ex.getMessage))
    }
  }
}
