import sbt._

object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "3.0.0",
    "uk.gov.hmrc" %% "auth-client" % "3.2.0-play-27",
    "uk.gov.hmrc" %% "play-ui" % "8.15.0-play-27",
    "uk.gov.hmrc" %% "play-partials" % "7.0.0-play-27",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-27",
    "uk.gov.hmrc" %% "http-caching-client" % "9.1.0-play-27",
    "com.typesafe.play" %% "play-json-joda" % "2.7.4",
    "uk.gov.hmrc" %% "govuk-template" % "5.59.0-play-27"
  )


  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
          "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % scope,
        "org.pegdown" % "pegdown" % "1.6.0",
        "org.jsoup" % "jsoup" % "1.13.1" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "3.3.3" % scope,
        "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
