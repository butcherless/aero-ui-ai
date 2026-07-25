package app.components

import com.raquo.laminar.api.L._

object Layout {

  /** @param content a Signal that emits the element to show in the main area, driven by the current Page */
  def apply(content: Signal[HtmlElement]): HtmlElement =
    div(
      cls := "app-shell",
      Sidebar(),
      div(
        cls := "app-main",
        TopBar(),
        div(
          cls := "app-content",
          child <-- content
        )
      )
    )
}
