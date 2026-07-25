package app.components

import com.raquo.laminar.api.L._

/** Generic searchable-list table shared by every entity page: a header row, a data row per item with a click handler,
  * and a highlighted row for whatever is currently selected.
  */
object EntityTable {

  def apply[T](
      columns: List[(String, T => String)],
      rows: Signal[List[T]],
      rowKey: T => String,
      selectedKey: Signal[Option[String]],
      onRowClick: T => Unit,
      loading: Signal[Boolean] = Val(false),
      error: Signal[Option[String]] = Val(None)
  ): HtmlElement = {

    val statusSignal: Signal[List[HtmlElement]] = error.combineWith(loading).map {
      case (Some(msg), _) => List(div(cls := "banner banner-error", msg))
      case (None, true) => List(div(cls := "status-line", "Loading…"))
      case (None, false) => Nil
    }

    div(
      cls := "entity-table-wrap",
      children <-- statusSignal,
      table(
        cls := "entity-table",
        thead(tr(columns.map { case (header, _) => th(header) })),
        tbody(
          children <-- rows.combineWith(selectedKey).map {
            case (list, selKeyOpt) =>
              if (list.isEmpty)
                List(tr(td(colSpan := columns.size, cls := "empty-row", "No results")))
              else
                list.map { item =>
                  val key = rowKey(item)
                  tr(
                    cls := (if (selKeyOpt.contains(key)) "entity-row selected" else "entity-row"),
                    onClick --> (_ => onRowClick(item)),
                    columns.map { case (_, render) => td(render(item)) }
                  )
                }
          }
        )
      )
    )
  }
}
