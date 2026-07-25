package app.components

import com.raquo.laminar.api.L._

/** Prev/page-number/Next bar for paginated lists. The backend's list responses carry no total count, so `hasNext` is a
  * heuristic supplied by the caller (typically: the last fetch returned a full page).
  */
object Pagination {

  def apply(page: Signal[Int], hasNext: Signal[Boolean], onPrev: () => Unit, onNext: () => Unit): HtmlElement =
    div(
      cls := "pagination-bar",
      button(
        cls := "btn btn-secondary",
        "Previous",
        disabled <-- page.map(_ <= 1),
        onClick --> (_ => onPrev())
      ),
      span(cls := "pagination-page", child.text <-- page.map(p => s"Page $p")),
      button(
        cls := "btn btn-secondary",
        "Next",
        disabled <-- hasNext.map(!_),
        onClick --> (_ => onNext())
      )
    )
}
