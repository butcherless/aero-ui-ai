import sbt.*
import org.scalajs.sbtplugin.ScalaJSCrossVersion

object Dependencies {

  // project/*.scala isn't compiled inside a settings/Def context, so the %%% macro (which infers
  // JS vs JVM vs Native from the enclosing crossProject platform) isn't usable here — .cross(...)
  // is %%%'s underlying, non-macro equivalent for a plain (non-cross) Scala.js project.
  private def sjs(organization: String, name: String, version: String): ModuleID =
    (organization %% name % version).cross(ScalaJSCrossVersion.binary)

  // Laminar + Scala.js runtime
  val laminar    = sjs("com.raquo", "laminar", Versions.laminar)
  val scalaJsDom = sjs("org.scala-js", "scalajs-dom", Versions.scalaJsDom)
  val upickle    = sjs("com.lihaoyi", "upickle", Versions.upickle)

  // Test — ScalaTest + Laminar's own (unpublished) domtestutils-based mounting approach
  val scalatest    = sjs("org.scalatest", "scalatest", Versions.scalatest) % Test
  val domtestutils = sjs("com.raquo", "domtestutils", Versions.domtestutils) % Test

  val commonTest: Seq[ModuleID] = Seq(scalatest, domtestutils)
}
