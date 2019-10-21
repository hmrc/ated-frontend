import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import sbt.{Def, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.SbtAutoBuildPlugin
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
  import uk.gov.hmrc.versioning.SbtGitVersioning

  val appName: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  def makeExcludedFiles(rootDir:File):Seq[String] = findPlayConfFiles(rootDir) ++ findSbtFiles(rootDir)

  def findSbtFiles(rootDir: File): Seq[String] = {
    if(rootDir.getName == "project") {
      rootDir.listFiles().map(_.getName).toSeq
    } else {
      Seq()
    }
  }
  def findPlayConfFiles(rootDir: File): Seq[String] = {
    Option { new File(rootDir, "conf").listFiles() }.fold(Seq[String]()) { confFiles =>
      confFiles
        .map(_.getName.replace(".routes", ".Routes"))
    }
  }

  lazy val scoverageSettings: Seq[Def.Setting[_ >: Boolean with Double with String]] = {
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages :=
        "<empty>;Reverse.*;views.html.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;forms.*;config.*;models.*;views.*",
      ScoverageKeys.coverageMinimum := 100,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true
    )
  }

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin) ++ plugins: _*)
    .settings(playSettings:  _*)
    .settings(majorVersion := 3)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(playSettings ++ scoverageSettings: _*)
    .settings(
      scalaVersion := "2.11.11",
      libraryDependencies ++= appDependencies,
      routesGenerator := InjectedRoutesGenerator,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesImport += "config.JodaLocalDateRoutes._"
    )
    .settings(
      resolvers += Resolver.bintrayRepo("hmrc", "releases"),
      resolvers += Resolver.jcenterRepo
    )
}

