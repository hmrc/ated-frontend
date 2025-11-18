/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.helpers.application

import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import repositories.CacheRepository
import repository.StubRepo
import test.helpers.wiremock.WireMockConfig

trait IntegrationApplication extends GuiceOneServerPerSuite with WireMockConfig {
  self: TestSuite =>

  val currentAppBaseUrl: String = "ated-frontend"
  val testAppUrl: String        = s"http://localhost:$port/ated"

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Map[String, Any](
      "play.http.router"                                      -> "testOnlyDoNotUseInAppConf.Routes",
      "mongo.uri"                                             -> "mongodb://localhost:27017/test-ated-frontend",
      "microservice.metrics.graphite.host"                    -> "localhost",
      "microservice.metrics.graphite.port"                    -> 2003,
      "microservice.metrics.graphite.prefix"                  -> "play.ated-frontend.",
      "microservice.metrics.graphite.enabled"                -> true,
      "microservice.services.auth.host"                      -> wireMockHost,
      "microservice.services.auth.port"                       -> wireMockPort,
      "microservice.services.business-customer-frontend.host"  -> wireMockHost,
      "microservice.services.business-customer-frontend.port"  -> wireMockPort,
      "microservice.services.business-tax-account.host"        -> wireMockHost,
      "microservice.services.business-tax-account.port"        -> wireMockPort,
      "microservice.services.agent-client-mandate-frontend.host"    -> wireMockHost,
      "microservice.services.agent-client-mandate-frontend.port"    -> wireMockPort,
      "microservice.services.tax-enrolments.host"                -> wireMockHost,
      "microservice.services.tax-enrolments.port"                -> wireMockPort,
      "microservice.services.ated-frontend.host"                 -> wireMockHost,
      "microservice.services.ated-frontend.port"                 -> wireMockPort,
      "microservice.services.ated.host"                          -> wireMockHost,
      "microservice.services.ated.port"                          -> wireMockPort,
      "microservice.services.session-cache.host"                 -> wireMockHost,
      "microservice.services.session-cache.port"                 -> wireMockPort,
      "microservice.services.cachable.session-cache.host"        -> wireMockHost,
      "microservice.services.cachable.session-cache.port"        -> wireMockPort,
      "microservice.services.etmp-hod.host"                      -> wireMockHost,
      "microservice.services.etmp-hod.port"                      -> wireMockPort,
      "microservice.services.auth.company-auth.host"             -> wireMockHost,
      "microservice.services.datastream.host"                    -> wireMockHost,
      "microservice.services.datastream.port"                    -> wireMockPort,
      "microservice.services.address-lookup.host"                -> wireMockHost,
      "microservice.services.address-lookup.port"                -> wireMockPort,
      "auditing.enabled"                                         -> false,
      "metrics.name"                                             -> "ated-frontend",
      "metrics.rateUnit"                                         -> "SECONDS",
      "metrics.durationUnit"                                     -> "SECONDS",
      "metrics.showSamples"                                      -> true,
      "metrics.jvm"                                              -> false,
      "metrics.enabled"                                          -> true,
      "play.filters.csrf.header.bypassHeaders.Csrf-Token"        -> "nocheck"
    ))
    .overrides(bind[CacheRepository].toInstance(new StubRepo()))
    .build()

}
