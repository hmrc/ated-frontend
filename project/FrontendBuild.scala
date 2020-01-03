import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "ated-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.3.0",
    "uk.gov.hmrc" %% "auth-client" % "2.32.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.5.0-play-26",
    "uk.gov.hmrc" %% "play-partials" % "6.9.0-play-26",
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-26",
    "uk.gov.hmrc" %% "http-caching-client" % "9.0.0-play-26",
    "com.typesafe.play" %% "play-json-joda" % "2.6.10",
    "uk.gov.hmrc" %% "govuk-template" % "5.44.0-play-26"
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % scope,
        "org.pegdown" % "pegdown" % "1.6.0",
        "org.jsoup" % "jsoup" % "1.8.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "3.1.0" % scope,
        "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}


