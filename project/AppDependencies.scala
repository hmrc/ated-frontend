
import sbt._

object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "5.16.0",
    "uk.gov.hmrc"       %% "play-ui"                    % "9.7.0-play-28",
    "uk.gov.hmrc"       %% "play-partials"              % "8.2.0-play-28",
    "uk.gov.hmrc"       %% "domain"                     % "6.2.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"        % "9.5.0-play-28",
    "com.typesafe.play" %% "play-json-joda"             % "2.7.4",
    "uk.gov.hmrc"       %% "govuk-template"             % "5.72.0-play-28"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "uk.gov.hmrc"            %% "bootstrap-test-play-28"      % "5.16.0"            % scope,
        "org.scalatestplus.play" %% "scalatestplus-play"          % "5.1.0"             % scope,
        "org.pegdown"            %  "pegdown"                     % "1.6.0"             % scope,
        "org.jsoup"              %  "jsoup"                       % "1.14.3"            % scope,
        "com.typesafe.play"      %% "play-test"                   % PlayVersion.current % scope,
        "org.scalatestplus"      %% "mockito-3-12"                % "3.2.10.0"          % scope,
        "org.mockito"            %  "mockito-core"                % "4.0.0"             % scope,
        "com.github.tomakehurst" %  "wiremock-jre8"               % "2.26.3"            % scope,
        "org.scalamock"          %% "scalamock-scalatest-support" % "3.6.0"             % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
