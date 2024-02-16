
import sbt._

object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "7.23.0",
    "uk.gov.hmrc"       %% "play-partials"              % "8.4.0-play-28",
    "uk.gov.hmrc"       %% "domain"                     % "8.3.0-play-28",
    "uk.gov.hmrc"       %% "http-caching-client"        % "10.0.0-play-28",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "7.21.0-play-28"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "uk.gov.hmrc"                  %% "bootstrap-test-play-28"      % "7.23.0"            % scope,
        "org.scalatestplus.play"       %% "scalatestplus-play"          % "5.1.0"             % scope,
        "org.jsoup"                    %  "jsoup"                       % "1.17.2"            % scope,
        "com.typesafe.play"            %% "play-test"                   % PlayVersion.current % scope,
        "org.scalatestplus"            %% "mockito-4-11" % "3.2.17.0"   % scope,
        "org.mockito" % "mockito-core" % "5.5.0"                        % scope,
        "com.github.tomakehurst"       %  "wiremock-jre8"               % "2.35.1"            % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"        % "2.15.2"            % scope
      )
    }.test
  }

  val jacksonVersion = "2.16.1"
  val jacksonDatabindVersion = "2.16.1"

  val jacksonOverrides = Seq(
    "com.fasterxml.jackson.core" % "jackson-core",
    "com.fasterxml.jackson.core" % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion)

  val jacksonDatabindOverrides = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion
  )

  val akkaSerializationJacksonOverrides = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
    "com.fasterxml.jackson.module" % "jackson-module-parameter-names",
    "com.fasterxml.jackson.module" %% "jackson-module-scala",
  ).map(_ % jacksonVersion)

  def apply(): Seq[ModuleID] = compile ++ jacksonDatabindOverrides ++ jacksonOverrides ++ akkaSerializationJacksonOverrides ++ Test()
}
