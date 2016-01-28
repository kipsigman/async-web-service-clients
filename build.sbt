import Dependencies._

name := "async-web-service-clients"

lazy val commonSettings = Seq(
  scalaVersion := "2.11.7",
  organization := "kipsigman.ws",
  version := "0.1.0"
)

lazy val webServiceClient = (project in file("web-service-client")).
  configs(IntegrationTest).
  settings(commonSettings: _*).
  settings(Defaults.itSettings: _*).
  settings(
    name := "web-service-client",
    libraryDependencies ++= di ++ dispatch ++ play ++ scalaTest
  )

lazy val googleApiClients = (project in file("google-api-clients")).
  dependsOn(webServiceClient).
  aggregate(webServiceClient).
  configs(IntegrationTest).
  settings(commonSettings: _*).
  settings(Defaults.itSettings: _*).
  settings(
    name := "google-api-clients",
    libraryDependencies ++= Seq(
      "com.google.apis" % "google-api-services-analytics" % "v3-rev116-1.20.0",
      "com.google.api-client" % "google-api-client-gson" % "1.20.0"
    ) ++ scalaTest
  )

lazy val salesforceApiClient = (project in file("salesforce-api-client")).
  dependsOn(webServiceClient).
  aggregate(webServiceClient).
  configs(IntegrationTest).
  settings(commonSettings: _*).
  settings(Defaults.itSettings: _*).
  settings(
    name := "salesforce-api-client",
    libraryDependencies ++= akka ++ scalaTest
  )
