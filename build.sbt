import org.scalajs.linker.interface.ModuleSplitStyle

val scala3 = "3.8.3"

name := "bassfretz"

inThisBuild(
  List(
    scalaVersion := scala3,
    scalacOptions ++= Seq(
      "-scalajs",
      "-deprecation",
      "-feature"
    )
  )
)

lazy val bassfretz = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withSourceMap(true)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("dev.cheleb.bassfretz")))
    },
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "dev.cheleb" %%% "threesjs" % "0.1.0",
      "com.raquo" %%% "laminar" % "18.0.0-M5",
      "org.scalameta" %%% "munit" % "1.3.1" % Test
    )
  )
