package app.components

import app.components.FormValues.*
import com.raquo.laminar.api.L.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class FormValuesSpec extends AnyFunSpec with Matchers {

  it("trims text and upper-cases codes") {
    Var("  Madrid ").trimmed shouldBe "Madrid"
    Var(" lemd ").code shouldBe "LEMD"
  }

  it("treats a blank optional field as None") {
    Var("   ").optional shouldBe None
    Var("   ").optionalCode shouldBe None
  }

  it("keeps a filled optional field, trimmed") {
    Var(" Iberia Express ").optional shouldBe Some("Iberia Express")
    Var(" ib ").optionalCode shouldBe Some("IB")
  }
}
