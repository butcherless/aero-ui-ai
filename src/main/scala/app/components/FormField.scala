package app.components

import com.raquo.laminar.api.L.*

/** Small label+input building blocks shared by every entity's detail/create form. */
object FormField {

  def text(fieldLabel: String, valueVar: Var[String], placeholderText: String = ""): HtmlElement =
    inputField("text", fieldLabel, valueVar, placeholderText)

  def number(fieldLabel: String, valueVar: Var[String], placeholderText: String = ""): HtmlElement =
    inputField("number", fieldLabel, valueVar, placeholderText)

  def password(fieldLabel: String, valueVar: Var[String], placeholderText: String = ""): HtmlElement =
    inputField("password", fieldLabel, valueVar, placeholderText)

  private def inputField(inputType: String, fieldLabel: String, valueVar: Var[String], placeholderText: String) =
    div(
      cls := "form-field",
      div(cls := "form-field-label", fieldLabel),
      input(
        cls := "form-input",
        typ := inputType,
        placeholder := placeholderText,
        controlled(value <-- valueVar.signal, onInput.mapToValue --> valueVar.writer)
      )
    )

  def readOnly(fieldLabel: String, valueText: String): HtmlElement =
    div(
      cls := "form-field",
      div(cls := "form-field-label", fieldLabel),
      div(cls := "form-static-value", valueText)
    )

  def errorBanner(errVar: Var[Option[String]]): HtmlElement =
    div(children <-- errVar.signal.map(_.toList.map(msg => div(cls := "banner banner-error", msg))))
}
