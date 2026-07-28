addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.7")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.7.0")

// Needed for `jsEnv := new JSDOMNodeJSEnv()` in build.sbt, so DOM-touching tests can run under Node/jsdom.
libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.1"
