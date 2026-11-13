package app.components

import com.raquo.laminar.api.L.*

/** Reading a text field's current value the way every form and filter does: trimmed, upper-cased for codes (IATA, ICAO,
  * ISO, registrations), and `None` for an optional field left blank.
  */
object FormValues {

  extension (field: Var[String]) {
    def trimmed: String = field.now().trim
    def code: String = trimmed.toUpperCase
    def optional: Option[String] = Option(trimmed).filter(_.nonEmpty)
    def optionalCode: Option[String] = optional.map(_.toUpperCase)
  }
}
