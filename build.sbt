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

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withSourceMap(true)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("dev.cheleb.mythreeapp")))
    },
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "dev.cheleb" %%% "threesjs" % "0.1.0",
      "com.raquo" %%% "laminar" % "17.2.1"
    )
  )
