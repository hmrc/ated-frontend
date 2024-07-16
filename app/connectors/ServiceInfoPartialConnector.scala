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
import models.requests.NavContent
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.StringContextOps
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ServiceInfoPartialConnector @Inject()(http: HttpClientV2, config: ApplicationConfig) extends Logging {


  lazy val btaNavLinksUrl: String = config.btaBaseUrl + "/business-account/partial/nav-links"

  def getNavLinks(implicit ec: ExecutionContext, hc : HeaderCarrier): Future[Option[NavContent]] = {
    http.get(url"$btaNavLinksUrl").execute[Option[NavContent]]
      .recover{
        case e =>
          logger.warn(s"[ServiceInfoPartialConnector][getNavLinks] - Unexpected error ${e.getMessage}")
          None
      }
  }
}
