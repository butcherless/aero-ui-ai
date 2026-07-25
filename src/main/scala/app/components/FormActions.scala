package app.components

import com.raquo.laminar.api.L._

/** Shared button rows for entity detail/create forms, so the labels and CSS classes for Save/Delete/Cancel/Close live
  * in one place instead of being retyped per entity page.
  */
object FormActions {

  def saveDeleteCancel(
      saving: Signal[Boolean],
      onSave: () => Unit,
      onDelete: () => Unit,
      onCancel: () => Unit
  ): HtmlElement =
    div(
      cls := "detail-actions",
      button(cls := "btn btn-primary", "Save", disabled <-- saving, onClick --> (_ => onSave())),
      button(cls := "btn btn-danger", "Delete", disabled <-- saving, onClick --> (_ => onDelete())),
      button(cls := "btn btn-secondary", "Cancel", onClick --> (_ => onCancel()))
    )

  def saveCancel(saving: Signal[Boolean], onSave: () => Unit, onCancel: () => Unit): HtmlElement =
    div(
      cls := "detail-actions",
      button(cls := "btn btn-primary", "Save", disabled <-- saving, onClick --> (_ => onSave())),
      button(cls := "btn btn-secondary", "Cancel", onClick --> (_ => onCancel()))
    )

  def close(onClose: () => Unit): HtmlElement =
    div(
      cls := "detail-actions",
      button(cls := "btn btn-secondary", "Close", onClick --> (_ => onClose()))
    )
}
