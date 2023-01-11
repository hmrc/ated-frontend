
import sbt._

object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "5.25.0",
    "uk.gov.hmrc"       %% "play-partials"              % "8.3.0-play-28",
    "uk.gov.hmrc"       %% "domain"                     % "8.1.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"        % "9.6.0-play-28",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.3",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "4.1.0-play-28"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "uk.gov.hmrc"                  %% "bootstrap-test-play-28"      % "5.25.0"            % scope,
        "org.scalatestplus.play"       %% "scalatestplus-play"          % "5.1.0"             % scope,
        "org.jsoup"                    %  "jsoup"                       % "1.15.3"            % scope,
        "com.typesafe.play"            %% "play-test"                   % PlayVersion.current % scope,
        "org.scalatestplus"            %% "mockito-3-12"                % "3.2.10.0"          % scope,
        "org.mockito"                  %  "mockito-core"                % "4.9.0"             % scope,
        "com.github.tomakehurst"       %  "wiremock-jre8"               % "2.35.0"            % scope,
        "org.scalamock"                %% "scalamock-scalatest-support" % "3.6.0"             % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"        % "2.14.1"            % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
