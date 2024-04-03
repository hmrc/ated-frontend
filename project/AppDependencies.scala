
import sbt._

object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val jacksonVersion = "2.17.0"
  val bootstrapVersion = "8.5.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-partials-play-30"       % "9.1.0",
    "uk.gov.hmrc"       %% "domain-play-30"              % "9.0.0",
    "uk.gov.hmrc"       %% "http-caching-client-play-30" % "11.2.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"  % "8.5.0"
  )

 val test: Seq[ModuleID] = Seq(
        "uk.gov.hmrc"                  %% "bootstrap-test-play-30"      % bootstrapVersion    % "test",
        "org.jsoup"                    %  "jsoup"                       % "1.17.2"            % "test",
        "org.scalatestplus"            %%  "mockito-4-11"               % "3.2.18.0"          % "test",
        "org.mockito"                  %  "mockito-core"                % "5.11.0"            % "test",
        "com.fasterxml.jackson.module" %% "jackson-module-scala"        % jacksonVersion      % "test"
      )

  val itDependencies: Seq[ModuleID] = Seq(
        "org.wiremock"                 %  "wiremock"                    % "3.3.1"             % Test
  )

  val jacksonOverrides = Seq(
    "com.fasterxml.jackson.core" % "jackson-core",
    "com.fasterxml.jackson.core" % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion)

  val jacksonDatabindOverrides = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
  )

  val akkaSerializationJacksonOverrides = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
    "com.fasterxml.jackson.module" % "jackson-module-parameter-names",
    "com.fasterxml.jackson.module" %% "jackson-module-scala",
  ).map(_ % jacksonVersion)

  def apply(): Seq[ModuleID] = compile ++ jacksonDatabindOverrides ++ jacksonOverrides ++ akkaSerializationJacksonOverrides ++ test
}
