package app.testkit

import com.raquo.domtestutils.scalatest.MountSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

/** Base class for synchronous component tests. Uses Laminar's own `render` (not a raw `appendChild`) so
  * Signal/Var-driven bindings actually activate, and relies on `com.raquo::domtestutils`'s `MountSpec` for the per-test
  * container reset/teardown — the closest thing to an official Laminar testing convention (mirrors raquo/Laminar's own
  * test suite, which pairs ScalaTest with this same library).
  */
abstract class LaminarMountSpec extends AnyFunSpec with MountSpec with Matchers with DomListOps {

  /** Mounts a Laminar element into the test container and returns its underlying DOM element. */
  def renderRoot(element: HtmlElement): dom.html.Element = {
    render(containerNode, element)
    rootNode.asInstanceOf[dom.html.Element]
  }
}
