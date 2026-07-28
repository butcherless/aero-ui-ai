package app.components

import com.raquo.laminar.api.L._
import org.scalajs.dom

/** A single search-input box that auto-fires `reload` after `debounceMs` of no typing, once the trimmed value reaches
  * `minLength` characters (or is cleared back to empty). Shared by every entity page's secondary filter box (Airports'
  * and Airlines' country filter, Aircraft's airline filter) — they differ only in placeholder text, threshold, and
  * which other box(es) to clear on focus.
  */
object DebouncedFilterInput {

  def apply(
      filterVar: Var[String],
      placeholderText: String,
      minLength: Int,
      reload: () => Unit,
      onFocusExtra: () => Unit = () => (),
      debounceMs: Int = 350
  ): HtmlElement =
    input(
      cls := "search-input",
      placeholder := placeholderText,
      controlled(value <-- filterVar.signal, onInput.mapToValue --> filterVar.writer),
      onFocus --> (_ => onFocusExtra()),
      onInput(_.debounce(debounceMs).map(_.target.asInstanceOf[dom.html.Input].value)) -->
        Observer[String] { v =>
          val trimmed = v.trim
          if (trimmed.isEmpty || trimmed.length >= minLength) reload()
        }
    )
}
