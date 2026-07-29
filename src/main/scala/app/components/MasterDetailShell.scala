package app.components

import com.raquo.laminar.api.L._

/** The Boceto 1 layout: page title, a center list panel (toolbar + table) and a right-hand detail/edit panel that swaps
  * content based on the current selection. The detail panel itself is only mounted while `detail` is `Some` — nothing
  * selected renders no panel at all, rather than an empty placeholder box.
  */
object MasterDetailShell {

  def apply(title: String, toolbar: HtmlElement, list: HtmlElement, detail: Signal[Option[HtmlElement]]): HtmlElement =
    div(
      cls := "page entity-page",
      h1(title),
      div(
        cls := "entity-layout",
        div(cls := "entity-list-panel", toolbar, list),
        child.maybe <-- detail.map(_.map(content => div(cls := "entity-detail-panel", content)))
      )
    )
}
