/*
 * Copyright 2023 HM Revenue & Customs
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

import config.ApplicationConfig
import javax.inject.Inject
import play.api.Logging
import play.api.mvc.Request
import play.twirl.api.Html
import play.utils.UriEncoding
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.play.partials.HtmlPartial.{Failure, Success}
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartialsConverter, HtmlPartial}

import scala.concurrent.{ExecutionContext, Future}

class AgentClientMandateFrontendConnector @Inject()(appConfig: ApplicationConfig,
                                                    httpClient: HttpClientV2)
  extends RawResponseReads with HeaderCarrierForPartialsConverter with Logging {

  val serviceUrl: String = appConfig.conf.baseUrl("agent-client-mandate-frontend")
  val returnUrlHost: String = appConfig.atedFrontendHost
  val clientBannerPartialUri = "internal/client/partial-banner"
  val clientDetailsUri = "mandate/client/details"

  def crypto: String => String = identity

  def getClientBannerPartial(clientId: String, service: String)
                            (implicit request: Request[_], ec: ExecutionContext): Future[HtmlPartial] = {
    val getUrl = s"$serviceUrl/$clientBannerPartialUri/$clientId/$service?returnUrl=$returnUrlHost${controllers.routes.AccountSummaryController.view}"

     httpClient.get(url"$getUrl").execute[HttpResponse] map { response =>
      response.status match {
        case s if s >= 200 && s <= 299 => Success(
          title = response.header("X-Title").map(UriEncoding.decodePathSegment(_, "UTF-8")),
          content = Html(response.body)
        )
        case s @ 404 =>
          Failure(Some(s), response.body)
        case other =>
          logger.warn(s"Failed to load partial from $getUrl, received $other")
          Failure(Some(other), response.body)
      }
    }
  }

  def getClientDetails(clientId: String, service: String)
                      (implicit request: Request[_], ec: ExecutionContext): Future[HttpResponse] = {
    val getUrl =
      s"$serviceUrl/$clientDetailsUri/$clientId/$service?returnUrl=$returnUrlHost${controllers.subscriptionData.routes.CompanyDetailsController.view}"

    httpClient.get(url"$getUrl").execute[HttpResponse]
  }
}
