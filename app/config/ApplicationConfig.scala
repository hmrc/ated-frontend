/*
 * Copyright 2021 HM Revenue & Customs
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

import config.featureswitch.FeatureSwitching
import config.{ConfigKeys => Keys}
import play.api.Environment
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.CountryCodeUtils

import javax.inject.Inject
import scala.util.Try

trait AppConfig {
  val btaBaseUrl: String
  val btaHomeUrl: String
  val btaMessagesUrl: String
  val btaManageAccountUrl: String
  val btaHelpAndContactUrl: String
}

class ApplicationConfig @Inject()(val conf: ServicesConfig,
                                  val environment: Environment,
                                  val templateError: views.html.global_error) extends CountryCodeUtils with AppConfig with FeatureSwitching {

  private def loadConfig(key: String) = conf.getString(key)

  private lazy val contactHost = conf.getString("contact-frontend.host")
  private lazy val helpAndContactFrontendUrl: String = conf.getString(Keys.helpAndContactFrontendBase)

  val contactFormServiceIdentifier = "ATED"
  override lazy val btaBaseUrl: String = conf.baseUrl(Keys.businessTaxAccountBase)
  override lazy val btaHomeUrl: String = conf.getString(Keys.businessTaxAccountHost) + conf.getString(Keys.businessTaxAccountUrl)
  override lazy val btaMessagesUrl: String = btaHomeUrl + conf.getString(Keys.businessTaxAccountMessagesUrl)
  override lazy val btaManageAccountUrl: String = btaHomeUrl + conf.getString(Keys.businessTaxAccountManageAccountUrl)
  override lazy val btaHelpAndContactUrl: String = helpAndContactFrontendUrl + conf.getString(Keys.helpAndContactHelpUrl)
  lazy val assetsPrefix: String = loadConfig("assets.url") + loadConfig("assets.version")
  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  lazy val analyticsToken: Option[String] = Try(conf.getString("google-analytics.token")).toOption
  lazy val analyticsHost: String = conf.getString("google-analytics.host")
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val atedFrontendHost: String = conf.getString("microservice.services.ated-frontend.host")
  lazy val defaultTimeoutSeconds: Int = loadConfig("defaultTimeoutSeconds").toInt
  lazy val timeoutCountdown: Int = loadConfig("timeoutCountdown").toInt
  lazy val urBannerToggle: Boolean = loadConfig("urBanner.toggle").toBoolean
  lazy val urBannerLink: String = loadConfig("urBanner.link")
  lazy val serviceSignOut:String = loadConfig("service-signout.url")

  lazy val baseUri: String = conf.baseUrl("cachable.session-cache")
  lazy val defaultSource: String = "ated-frontend"
  lazy val domain: String = conf.getConfString(
    "cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'")
  )

  lazy val basGatewayHost: String = s"${conf.getString("microservice.services.auth.bas-gateway-frontend.host")}"
  lazy val loginCallback: String = conf.getString("microservice.services.auth.login-callback.url")
  lazy val loginPath: String = s"${conf.getString("microservice.services.auth.login-path")}"
  lazy val loginURL: String = s"$basGatewayHost/bas-gateway/$loginPath"
  lazy val continueURL: String = s"$loginCallback"
  lazy val signIn: String = s"$basGatewayHost/bas-gateway/$loginPath?continue_url=$loginCallback"
  lazy val signOut: String = s"$basGatewayHost/bas-gateway/sign-out-without-state"
  lazy val signOutRedirect: String = conf.getString("microservice.services.auth.sign-out-redirect")
  lazy val createNewGatewayLink: String = conf.getString("microservice.services.auth.create-account")
  lazy val subscriptionStartPage: String = conf.getString("microservice.services.ated-subscription.serviceRedirectUrl")
  lazy val clientApproveAgentMandate: String = conf.getString("microservice.services.agent-client-mandate-frontend.atedClientApproveAgentUri")
  lazy val agentRedirectedToMandate: String = conf.getString("microservice.services.agent-client-mandate-frontend.atedAgentJourneyStartUri")
  lazy val atedPeakStartDay: String = conf.getString(key = "atedPeakStartDay")

}
