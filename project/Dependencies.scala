import sbt._

object Dependencies {

  lazy val playVersion = "2.5.3"
  lazy val akkaVersion	= "2.4.4"

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion
  )

  lazy val di = Seq(
    "javax.inject" % "javax.inject" % "1",
    "com.google.inject" % "guice" % "4.0"
  )
  
  lazy val play = Seq(
    "com.typesafe.play" %% "play" % playVersion,
    "com.typesafe.play" %% "play-json" % playVersion
  )

  lazy val dispatch = Seq(
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.3"
  )
 
  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % "test,it",
    "org.mockito" % "mockito-core" % "1.10.19" % "test,it"
  )

}
