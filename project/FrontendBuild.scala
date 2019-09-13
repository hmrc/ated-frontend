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
    "uk.gov.hmrc" %% "frontend-bootstrap" % "12.4.0",
    "uk.gov.hmrc" %% "auth-client" % "2.19.0-play-25",
    "uk.gov.hmrc" %% "play-partials" % "6.5.0",
    "uk.gov.hmrc" %% "domain" % "5.3.0",
    "uk.gov.hmrc" %% "http-caching-client" % "8.1.0"
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
        "org.pegdown" % "pegdown" % "1.6.0",
        "org.jsoup" % "jsoup" % "1.8.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-all" % "1.10.19" % scope,
        "uk.gov.hmrc" %% "hmrctest" % "3.5.0-play-25" % scope
      )
    }.test

  }

  def apply() = compile ++ Test()
}


