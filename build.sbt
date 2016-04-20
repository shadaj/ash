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
    scalaVersion := "2.11.8",
    libraryDependencies += "me.chrons" %%% "boopickle" % "1.1.3",
    sourceGenerators in Compile <+= resourceGenerator("gen", "main", Seq("me", "shadaj", "ash"))
  )

lazy val motorRemote = project.in(file("motor-remote"))

lazy val ashJS = ash.js

val sjsFiles = Def.taskDyn {
  (fastOptJS in (ashJS, Compile)).map { _ =>
    val root = (crossTarget in ashJS).value / "server-resources" / "META-INF" / "resources"
    Seq(
      root,
      root / "sjs",
      root / "sjs" / "ash-fastopt.js",
      root / "sjs" / "ash-jsdeps.js",
      root / "sjs" / "ash-jsdeps.min.js",
      root / "sjs" / "ash-launcher.js"
    )
  }
}

lazy val ashJVM = ash.jvm.settings(
  resourceDirectories in Compile ++= Seq(
    (crossTarget in ashJS).value / "server-resources"
  ),
  managedResources in Compile ++= sjsFiles.value
).dependsOn(motorRemote)
