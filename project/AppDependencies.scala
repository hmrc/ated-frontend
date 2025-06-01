import sbt.*
import play.sbt.PlayImport.*

object AppDependencies {

  val bootstrapVersion = "9.11.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-partials-play-30"       % "10.0.0",
    "uk.gov.hmrc"       %% "domain-play-30"              % "10.0.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"  % "12.0.0",
    "uk.gov.hmrc"       %% "http-caching-client-play-30" % "12.1.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"          % "2.6.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % "2.6.0"          % Test
  )

  val itDependencies: Seq[ModuleID] = Seq()

  def apply(): Seq[ModuleID] = compile ++ test
}
