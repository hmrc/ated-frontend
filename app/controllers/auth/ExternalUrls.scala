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

package controllers.auth

import play.api.Play
import play.api.Play.current

object ExternalUrls {

  val companyAuthHost: String = s"${Play.configuration.getString("microservice.services.auth.company-auth.host").getOrElse("")}"
  val loginCallback: String = Play.configuration.getString("microservice.services.auth.login-callback.url").getOrElse("/ated/home")
  val loginPath: String = s"${Play.configuration.getString("microservice.services.auth.login-path").getOrElse("sign-in")}"
  val loginURL: String = s"$companyAuthHost/gg/$loginPath"
  val continueURL: String = s"$loginCallback"
  val signIn: String = s"$companyAuthHost/gg/$loginPath?continue=$loginCallback"
  val signOut: String = s"$companyAuthHost/gg/sign-out"
  val subscriptionStartPage: String = Play.configuration.getString("microservice.services.ated-subscription.serviceRedirectUrl")
    .getOrElse("/ated-subscription/start")
  val clientApproveAgentMandate: String = Play.configuration
    .getString("microservice.services.agent-client-mandate-frontend.atedClientApproveAgentUri").getOrElse("/mandate/client/email")
  val agentRedirectedToMandate: String = Play.configuration
    .getString("microservice.services.agent-client-mandate-frontend.atedAgentJourneyStartUri").getOrElse("/mandate/agent/service")
  val businessTaxAccountPage: String = s"${Play.configuration.getString("microservice.services.auth.business-tax-account.serviceRedirectUrl").getOrElse("")}"
  
}
