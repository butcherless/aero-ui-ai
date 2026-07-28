package app.components
import com.raquo.laminar.api.L._
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future
import scala.concurrent.Promise

class AsyncActionSpec extends AsyncFunSpec with Matchers {

  it("flips saving on immediately, then off and calls onSuccess once the action succeeds") {
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])
    val promise = Promise[String]()
    var result: Option[String] = None

    AsyncAction.run(savingVar, errVar)(promise.future)(value => result = Some(value))
    savingVar.now() shouldBe true

    promise.success("done")

    promise.future.map { _ =>
      savingVar.now() shouldBe false
      errVar.now() shouldBe None
      result shouldBe Some("done")
    }
  }

  it("flips saving off and sets the error message from the exception on failure") {
    val savingVar = Var(false)
    val errVar = Var(Option.empty[String])
    val promise = Promise[String]()

    AsyncAction.run(savingVar, errVar)(promise.future)(_ => ())

    promise.failure(new RuntimeException("boom"))

    // transformWith (not map) since this branch settles the future as a failure.
    promise.future.transformWith(_ => Future.successful(())).map { _ =>
      savingVar.now() shouldBe false
      errVar.now() shouldBe Some("boom")
    }
  }

  it("clears a previous error as soon as a new run starts") {
    val savingVar = Var(false)
    val errVar = Var(Option[String]("stale error"))
    val promise = Promise[String]()

    AsyncAction.run(savingVar, errVar)(promise.future)(_ => ())
    errVar.now() shouldBe None

    promise.success("ok")
    promise.future.map(_ => succeed)
  }
}
