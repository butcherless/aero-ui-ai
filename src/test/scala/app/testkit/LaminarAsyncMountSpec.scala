package app.testkit

import com.raquo.domtestutils.scalatest.AsyncMountSpec
import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

/** Async variant of [[LaminarMountSpec]], for tests that need to await a `Future` (e.g. the fetch-then-render behavior
  * in `EntityCrudPage`), using the same real-DOM-mount approach.
  */
abstract class LaminarAsyncMountSpec extends AsyncFunSpec with AsyncMountSpec with Matchers with DomListOps {

  // The app runs its Future callbacks on MacrotaskExecutor (see Http). Assertions chained off the same Future must use
  // it too: they then queue behind the app's own callbacks instead of jumping ahead of them on the microtask queue.
  implicit override def executionContext: ExecutionContext = MacrotaskExecutor

  def renderRoot(element: HtmlElement): dom.html.Element = {
    render(containerNode, element)
    rootNode.asInstanceOf[dom.html.Element]
  }
}
