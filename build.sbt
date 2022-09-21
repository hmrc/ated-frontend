import TestPhases.{TemplateItTest, TemplateTest}
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "ated-frontend"

lazy val appDependencies: Seq[ModuleID] = AppDependencies()
lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala)
lazy val playSettings: Seq[Setting[_]] = Seq.empty

lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;app.Routes.*;prod.*;testOnlyDoNotUseInAppConf.*;uk.gov.hmrc.BuildInfo*;.*MicroserviceAuditConnector*;.*MicroserviceAuthConnector*;.*WSHttp*;uk.gov.hmrc.agentclientmandate.config.*;",
      ScoverageKeys.coverageMinimumStmtTotal := 80,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }

val silencerVersion = "1.7.6"

lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins: _*)
    .settings(playSettings: _*)
    .settings(majorVersion := 3)
    .configs(IntegrationTest)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(scalaVersion := "2.12.15")
    .settings(playSettings ++ scoverageSettings: _*)
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
    .settings(
      TwirlKeys.templateImports ++= Seq(
        "views.html.helper.form",
        "uk.gov.hmrc.play.views.html.helpers.FormWithCSRF"
      ),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      inConfig(IntegrationTest)(Defaults.itSettings),
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      routesGenerator := InjectedRoutesGenerator,
      Test / parallelExecution   := true,
      Test / fork                := true,
      IntegrationTest / Keys.fork :=  false,
      IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
      IntegrationTest / parallelExecution := false,
      routesImport += "config.JodaLocalDateRoutes._"
    )
    .disablePlugins(JUnitXmlReportPlugin)
    .settings(
      resolvers += Resolver.jcenterRepo,
      scalacOptions += "-P:silencer:pathFilters=views;routes",
      libraryDependencies ++= Seq(
        compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
        "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
      )
    )
    .disablePlugins(JUnitXmlReportPlugin)
