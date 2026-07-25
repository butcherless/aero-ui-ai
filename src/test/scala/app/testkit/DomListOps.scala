package app.testkit

import org.scalajs.dom

/** domtestutils' `ExpectedNode`/`RuleImplicits` DSL needs framework-specific glue that Laminar doesn't publish (only
  * Laminar's own *internal*, unpublished test sources wire it up), so component tests here assert directly against the
  * rendered DOM instead. This is the one bit of plumbing that needs: `NodeList` doesn't support `.toList` directly.
  */
trait DomListOps {

  def toList[T <: dom.Node](list: dom.NodeList[T]): List[T] = {
    var result: List[T] = Nil
    var i = 0
    while (i < list.length) {
      result = result :+ list(i)
      i += 1
    }
    result
  }
}
