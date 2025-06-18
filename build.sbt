/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.*
import sbt.Keys.*
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "ated-frontend"

ThisBuild / majorVersion := 3
ThisBuild / scalaVersion := "2.13.16"

lazy val appDependencies: Seq[ModuleID] = AppDependencies()
lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala)
lazy val playSettings: Seq[Setting[?]] = Seq.empty

lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages :=
        "<empty>;" +
          "Reverse.*;" +
          "app.Routes.*;" +
          "prod.*;" +
          "testOnlyDoNotUseInAppConf.*;" +
          "config.*;" +
          "uk.gov.hmrc.BuildInfo*;" +
          ".*MicroserviceAuditConnector*;" +
          ".*MicroserviceAuthConnector*;" +
          ".*WSHttp*;" +
          "uk.gov.hmrc.agentclientmandate.config.*;",
      ScoverageKeys.coverageMinimumStmtTotal := 80,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }

lazy val microservice = Project(appName, file("."))
    .enablePlugins((Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins) *)
    .settings(playSettings *)
    .settings(scalaSettings *)
    .settings(defaultSettings() *)
    .settings((playSettings ++ scoverageSettings) *)
    .settings(
      TwirlKeys.templateImports ++= Seq(
        "views.html.helper.form",
        "uk.gov.hmrc.govukfrontend.views.html.components._",
        "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._",
        "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
      ),
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      routesGenerator := InjectedRoutesGenerator,
      Test / parallelExecution   := true,
      Test / fork                := true,
      routesImport += "config.JavaLocalDateRoutes._"
    )
    .disablePlugins(JUnitXmlReportPlugin)
    .settings(
      scalacOptions += "-Wconf:src=routes/.*:s",
      scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s"
    )
    .disablePlugins(JUnitXmlReportPlugin)


lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.itDependencies)
