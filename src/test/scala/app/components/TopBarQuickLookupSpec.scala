package app.components

import app.testkit.LaminarAsyncMountSpec
import org.scalajs.dom

import scala.concurrent.Future
import scala.concurrent.Promise

/** Covers the exact-code quick-lookup widget in `TopBar` — untouched by `TopBarSpec`, which only exercises the
  * logo/avatar/username/disconnect. jsdom has no `fetch` global (confirmed by the `Http.rawRequest` synchronous-throw
  * fix this suite's `HttpSpec`/prior debugging already relies on), so any lookup here deterministically fails with "No
  * match" — that failure path, not the unreachable success path, is what's actually testable without a real backend.
  * Async because the failure only lands after the underlying Future settles; `nextTick` lets pending microtask chains
  * drain via a macrotask boundary before assertions run.
  */
class TopBarQuickLookupSpec extends LaminarAsyncMountSpec {

  private def nextTick(): Future[Unit] = {
    val p = Promise[Unit]()
    dom.window.setTimeout(() => p.success(()), 0)
    p.future
  }

  private def settle(): Future[Unit] = nextTick().flatMap(_ => nextTick()).flatMap(_ => nextTick())

  it("shows a \"No match\" result once a lookup fails") {
    val root = renderRoot(TopBar())
    val input = root.querySelector(".quick-lookup-input").asInstanceOf[dom.html.Input]
    input.value = "ES"
    input.dispatchEvent(new dom.Event("input"))

    val goButton = toList(root.querySelectorAll(".quick-lookup-row button")).head.asInstanceOf[dom.html.Element]
    goButton.click()

    settle().map { _ =>
      val result = root.querySelector(".quick-lookup-result")
      result should not be null
      result.textContent should include("No match for \"ES\"")
      result.getAttribute("class") should include("quick-lookup-error")
    }
  }

  it("clears the previous result when the lookup kind is changed") {
    val root = renderRoot(TopBar())
    val input = root.querySelector(".quick-lookup-input").asInstanceOf[dom.html.Input]
    input.value = "ES"
    input.dispatchEvent(new dom.Event("input"))
    toList(root.querySelectorAll(".quick-lookup-row button")).head.asInstanceOf[dom.html.Element].click()

    settle().map { _ =>
      root.querySelector(".quick-lookup-result") should not be null

      val select = root.querySelector(".quick-lookup-select").asInstanceOf[dom.html.Select]
      select.value = "airport"
      select.dispatchEvent(new dom.Event("change"))

      root.querySelector(".quick-lookup-result") shouldBe null
    }
  }

  it("does not run a lookup when the code is left empty") {
    val root = renderRoot(TopBar())

    toList(root.querySelectorAll(".quick-lookup-row button")).head.asInstanceOf[dom.html.Element].click()

    settle().map { _ =>
      root.querySelector(".quick-lookup-result") shouldBe null
    }
  }
}
