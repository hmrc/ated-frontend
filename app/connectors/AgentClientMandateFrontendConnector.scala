/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors

import config.{ApplicationConfig, WSHttp}
import play.api.mvc.Request
import uk.gov.hmrc.http.{CoreGet, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartialsConverter, HtmlPartial}

import scala.concurrent.Future

trait AgentClientMandateFrontendConnector extends ServicesConfig with RawResponseReads with HeaderCarrierForPartialsConverter {

  def serviceUrl: String = baseUrl("agent-client-mandate-frontend")
  def returnUrlHost: String = ApplicationConfig.atedFrontendHost
  val http: CoreGet = WSHttp
  val clientBannerPartialUri = "mandate/client/partial-banner"
  val clientDetailsUri = "mandate/client/details"

  def getClientBannerPartial(clientId: String, service: String)(implicit request: Request[_]): Future[HtmlPartial] = {
    val getUrl = s"$serviceUrl/$clientBannerPartialUri/$clientId/$service?returnUrl=" + returnUrlHost + controllers.routes.AccountSummaryController.view()
    http.GET[HtmlPartial](getUrl)
  }

  def getClientDetails(clientId: String, service: String)(implicit request: Request[_]): Future[HttpResponse] = {
    val getUrl =
      s"$serviceUrl/$clientDetailsUri/$clientId/$service?returnUrl=" + returnUrlHost + controllers.subscriptionData.routes.CompanyDetailsController.view()
    http.GET[HttpResponse](getUrl)
  }
}

object AgentClientMandateFrontendConnector extends AgentClientMandateFrontendConnector{
  // $COVERAGE-OFF$Trivial and never going to be called by a test that uses it's own object implementation
  override val crypto = SessionCookieCryptoFilter.encrypt _
  // $COVERAGE-ON$
}
