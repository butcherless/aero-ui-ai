package app.testkit

import com.raquo.domtestutils.scalatest.AsyncMountSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers

/** Async variant of [[LaminarMountSpec]], for tests that need to await a `Future` (e.g. the fetch-then-render behavior
  * in `EntityCrudPage`), using the same real-DOM-mount approach.
  */
abstract class LaminarAsyncMountSpec extends AsyncFunSpec with AsyncMountSpec with Matchers with DomListOps {

  def renderRoot(element: HtmlElement): dom.html.Element = {
    render(containerNode, element)
    rootNode.asInstanceOf[dom.html.Element]
  }
}
