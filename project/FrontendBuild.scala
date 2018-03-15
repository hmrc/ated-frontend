import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "ated-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val playHealthVersion = "2.1.0"

  private val frontendBootstrapVersion = "8.19.0"
  private val httpCachingClientVersion = "7.0.0"
  private val playPartialsVersion = "6.1.0"
  private val domainVersion = "4.1.0"
  private val hmrcTestVersion = "2.3.0"
  private val scalaTestPlusVersion = "1.5.1"
  private val pegdownVersion = "1.6.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.8",
    "com.codahale.metrics" % "metrics-graphite" % "3.0.2"
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion,
        "org.jsoup" % "jsoup" % "1.8.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope
      )
    }.test

  }

  def apply() = compile ++ Test()
}


