import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv

ThisBuild / scalaVersion := "3.3.8"

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
    scalacOptions += "-Wunused:all", // required by scalafix's RemoveUnused rule

    libraryDependencies ++= Seq(
      "com.raquo"     %%% "laminar"      % "17.2.1",
      "org.scala-js"  %%% "scalajs-dom"  % "2.8.1",
      "com.lihaoyi"   %%% "upickle"      % "4.4.3",
      "org.scalatest" %%% "scalatest"    % "3.2.20" % Test,
      "com.raquo"     %%% "domtestutils" % "19.0.0" % Test
    )
  )
