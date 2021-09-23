name := """TaskManager"""
organization := "com"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.6"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice ,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  "io.jvm.uuid" %% "scala-uuid" % "0.3.1",
  "com.github.jwt-scala" %% "jwt-core" % "7.1.4",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0",
  "org.scalatestplus" %% "mockito-3-4" % "3.2.9.0" % Test)




// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.binders._"
