libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.0"
libraryDependencies += "com.github.japgolly.scalajs-react" %%% "core" % "0.11.0"
libraryDependencies += "me.shadaj" %%% "appa" % "0.1.0-SNAPSHOT"
libraryDependencies += "com.payalabs" %%% "scalajs-react-mdl" % "0.2.0-SNAPSHOT"

jsDependencies ++= Seq(
  ProvidedJS / "vibrant.min.js" commonJSName "Vibrant",
  "org.webjars.bower" % "react" % "15.0.1" / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
  "org.webjars.bower" % "react" % "15.0.1" / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM"
)

jsEnv := PhantomJSEnv(autoExit = false).value

persistLauncher in Compile := true

Seq(packageScalaJSLauncher, fullOptJS, fastOptJS, packageJSDependencies).map { packageJSKey =>
  crossTarget in(Compile, packageJSKey) := crossTarget.value / "server-resources" / "META-INF" / "resources" / "sjs"
}