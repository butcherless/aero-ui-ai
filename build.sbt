import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import Dependencies.*

addCommandAlias("xdup", "dependencyUpdates")
addCommandAlias("fmt", "scalafmtAll")

// Reload the build automatically when project/*.scala or build.sbt change, instead of
// just warning and requiring a manual `reload`.
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := Versions.scala3

ThisBuild / scalacOptions := Seq(
  "-encoding",
  "utf8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wunused:all" // required by scalafix's RemoveUnused rule
)

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "aero-ui-ai",
    scalaJSUseMainModuleInitializer := true,

    // Emit the compiled JS straight into public/js so Vite can serve it as a static asset
    Compile / fastLinkJS / scalaJSLinkerOutputDirectory := baseDirectory.value / "public" / "js",
    Compile / fullLinkJS / scalaJSLinkerOutputDirectory := baseDirectory.value / "public" / "js",

    // Run tests under Node + jsdom so DOM-touching specs (mounting Laminar elements) can execute
    jsEnv := new JSDOMNodeJSEnv(),

    semanticdbEnabled := true, // required by scalafix
    semanticdbVersion := scalafixSemanticdb.revision,

    libraryDependencies ++= Seq(laminar, scalaJsDom, upickle) ++ commonTest
  )
