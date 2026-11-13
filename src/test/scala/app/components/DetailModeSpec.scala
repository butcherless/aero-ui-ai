package app.components

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class DetailModeSpec extends AnyFunSpec with Matchers {

  describe("DetailMode.editing") {
    it("is the edited item only while editing") {
      List[(DetailMode[String], Option[String])](
        DetailMode.NoSelection -> None,
        DetailMode.Creating -> None,
        DetailMode.Editing("MAD") -> Some("MAD")
      ).foreach {
        case (mode, expected) =>
          withClue(mode) { mode.editing shouldBe expected }
      }
    }
  }
}
