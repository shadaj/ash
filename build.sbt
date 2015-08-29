import scala.io.Source

organization := "me.shadaj"

name := "ash"

lazy val ashRoot = project.in(file(".")).
  aggregate(ashJS, ashJVM, motorRemote).
  settings(
    publish := {},
    publishLocal := {}
  )

def resourceGenerator(folder: String, sourceType: String, outputPackage: Seq[String]) = {
  baseDirectory map { dir =>
    val fileToWrite = dir / ".." / "shared" / "src" / folder / "scala" / outputPackage.mkString("/") / "Resources.scala"
    val folderToRead = dir / ".." / "shared" / "src" / sourceType / "resources"

    def sourceForDir(directory: File): String = {
      directory.listFiles().map { file =>
        if (file.isDirectory) {
          s"""object ${file.name} {
              |${sourceForDir(file)}
              |}""".stripMargin
        } else {
          val fileLines = Source.fromFile(file).getLines().toList
          val stringList = fileLines.map(s => '"' + s + '"').toString()
          s"""val ${file.name.split('.').head} = $stringList"""
        }
      }.mkString("\n")
    }

    val toWrite =
      s"""package ${outputPackage.mkString(".")}
          |object Resources {
          |${sourceForDir(folderToRead)}
          |}""".stripMargin
    IO.write(fileToWrite, toWrite)
    Seq(fileToWrite)
  }
}

lazy val ash = crossProject.in(file(".")).
  settings(
    name := "ash",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.7",
    libraryDependencies += "me.chrons" %%% "boopickle" % "1.0.0",
    sourceGenerators in Compile <+= resourceGenerator("gen", "main", Seq("me", "shadaj", "ash"))
  ).
  jvmSettings(
    libraryDependencies += "me.shadaj" %% "spotify-scala" % "0.1.0-SNAPSHOT",
    libraryDependencies += "io.netty" % "netty" % "3.9.2.Final" force(),
    libraryDependencies += ws
  ).
  jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0",
    libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.5.2",
    libraryDependencies += "com.timushev" %%% "scalatags-rx" % "0.1.0",
    libraryDependencies += "me.shadaj" %%% "appa" % "0.1.0-SNAPSHOT",
    jsDependencies += ProvidedJS / "Vibrant.min.js",
    preLinkJSEnv := PhantomJSEnv(autoExit = false).value,
    postLinkJSEnv := PhantomJSEnv(autoExit = false).value,
    persistLauncher in Compile := true
  )


lazy val motorRemote = project.in(file("motor-remote")).enablePlugins(AssemblyPlugin).settings(
  scalaVersion := "2.11.6",
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.12",
  libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.3.12"
)

lazy val ashJS = ash.js
lazy val ashJVM = ash.jvm.enablePlugins(PlayScala).settings(
  Seq(packageScalaJSLauncher, fastOptJS, fullOptJS, packageJSDependencies) map { packageJSKey =>
    crossTarget in (ashJS, Compile, packageJSKey) := (crossTarget in ashJS).value / "sjs"
  },
  unmanagedResourceDirectories in Assets += (crossTarget in ashJS).value / "sjs",
  compile in Compile <<= (compile in Compile) dependsOn (fastOptJS in (ashJS, Compile)),
  stage <<= stage dependsOn (fullOptJS in (ashJS, Compile))
).dependsOn(motorRemote)
