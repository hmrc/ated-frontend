/*
 * Copyright 2019 HM Revenue & Customs
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

package config

import javax.inject.Inject
import play.api.Environment
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.CountryCodeUtils

import scala.util.Try

class ApplicationConfig @Inject()(val conf: ServicesConfig,
                                  val environment: Environment) extends CountryCodeUtils {

  private def loadConfig(key: String) = conf.getString(key)

  private lazy val contactHost = conf.getString("contact-frontend.host")

  val contactFormServiceIdentifier = "ATED"

  lazy val assetsPrefix: String = loadConfig("assets.url") + loadConfig("assets.version")
  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  lazy val analyticsToken: Option[String] = Try{conf.getString("google-analytics.token")}.toOption
  lazy val analyticsHost: String = conf.getString("google-analytics.host")
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val atedFrontendHost: String = conf.getString("microservice.services.ated-frontend.host")
  lazy val defaultTimeoutSeconds: Int = loadConfig("defaultTimeoutSeconds").toInt
  lazy val timeoutCountdown: Int = loadConfig("timeoutCountdown").toInt
  lazy val urBannerToggle:Boolean = loadConfig("urBanner.toggle").toBoolean
  lazy val urBannerLink: String = loadConfig("urBanner.link")
  lazy val serviceSignOut:String = loadConfig("service-signout.url")

  lazy val baseUri: String = conf.baseUrl("cachable.session-cache")
  lazy val defaultSource: String = "ated-frontend"
  lazy val domain: String = conf.getConfString(
    "cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'")
  )

  lazy val companyAuthHost: String = s"${conf.getString("microservice.services.auth.company-auth.host")}"
  lazy val loginCallback: String = conf.getString("microservice.services.auth.login-callback.url")
  lazy val loginPath: String = s"${conf.getString("microservice.services.auth.login-path")}"
  lazy val loginURL: String = s"$companyAuthHost/gg/$loginPath"
  lazy val continueURL: String = s"$loginCallback"
  lazy val signIn: String = s"$companyAuthHost/gg/$loginPath?continue=$loginCallback"
  lazy val signOut: String = s"$companyAuthHost/gg/sign-out"
  lazy val subscriptionStartPage: String = conf.getString("microservice.services.ated-subscription.serviceRedirectUrl")
  lazy val clientApproveAgentMandate: String = conf.getString("microservice.services.agent-client-mandate-frontend.atedClientApproveAgentUri")
  lazy val agentRedirectedToMandate: String = conf.getString("microservice.services.agent-client-mandate-frontend.atedAgentJourneyStartUri")
  lazy val businessTaxAccountPage: String = s"${conf.getString("microservice.services.auth.business-tax-account.serviceRedirectUrl")}"

}
