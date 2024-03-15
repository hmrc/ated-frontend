import TestPhases.{TemplateItTest, TemplateTest}
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "ated-frontend"

lazy val appDependencies: Seq[ModuleID] = AppDependencies()
lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala)
lazy val playSettings: Seq[Setting[_]] = Seq.empty
lazy val silencerVersion = "1.7.14"

lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;app.Routes.*;prod.*;testOnlyDoNotUseInAppConf.*;config.*;uk.gov.hmrc.BuildInfo*;.*MicroserviceAuditConnector*;.*MicroserviceAuthConnector*;.*WSHttp*;uk.gov.hmrc.agentclientmandate.config.*;",
      ScoverageKeys.coverageMinimumStmtTotal := 80,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }

lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins: _*)
    .settings(playSettings: _*)
    .settings(majorVersion := 3)
    .configs(IntegrationTest)
    .settings(scalaSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(scalaVersion := "2.13.8")
    .settings(playSettings ++ scoverageSettings: _*)
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
    .settings(
      TwirlKeys.templateImports ++= Seq(
        "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._",
        "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
        "uk.gov.hmrc.govukfrontend.views.html.components._",
      ),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      inConfig(IntegrationTest)(Defaults.itSettings),
      libraryDependencies ++= appDependencies,
      ThisBuild / libraryDependencySchemes ++= Seq(
        "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
      ),
      libraryDependencies ++= Seq(
        compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
        "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
      ),
      retrieveManaged := true,
      routesGenerator := InjectedRoutesGenerator,
      Test / parallelExecution   := true,
      Test / fork                := true,
      IntegrationTest / Keys.fork :=  false,
      IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
      IntegrationTest / parallelExecution := false,
      routesImport += "config.JavaLocalDateRoutes._"
    )
    .disablePlugins(JUnitXmlReportPlugin)
    .settings(
      resolvers += Resolver.jcenterRepo,
      scalacOptions +=  "-feature",
      scalacOptions += "-Wconf:src=routes/.*:s",
      scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s"
    )
    //.disablePlugins(JUnitXmlReportPlugin)
