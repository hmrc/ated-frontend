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
import models.{AddressLookup, AddressLookupRecord}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.StringContextOps
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupConnector @Inject()(appConf: ApplicationConfig, http: HttpClientV2)(implicit ec: ExecutionContext) extends RawResponseReads with Logging {
  val serviceURL: String = appConf.conf.baseUrl("address-lookup")
  private val LOOKUP = "/lookup"
  private val UPRN = "/by-uprn"


  def findByPostcode(addressLookup: AddressLookup)(implicit hc: HeaderCarrier):Future[List[AddressLookupRecord]] = {
    http.post(url"$serviceURL$LOOKUP").withBody(Json.toJson(addressLookup)).execute[List[AddressLookupRecord]].recover {
      case e : UpstreamErrorResponse => {
        logger.warn(s"[AddressLookupConnector] [findbyPoscode] - Upstream error: ${e.reportAs} message: ${e.getMessage()}")
        Nil
      }
      case e => {
        logger.warn(s"[AddressLookupConnector] [findbyPoscode] - Error: ${e.getMessage}")
        Nil
      }
    }
  }

  def findById(uprn: String)(implicit hc: HeaderCarrier):Future[List[AddressLookupRecord]] = {
    http.post(url"$serviceURL$LOOKUP$UPRN").withBody(Json.obj("uprn" -> uprn)).execute[List[AddressLookupRecord]].recover {
      case e : UpstreamErrorResponse => {
        logger.warn(s"[AddressLookupConnector] [findById] - Upstream error: ${e.reportAs} message: ${e.getMessage()}")
        Nil
      }
      case e => {
        logger.warn(s"[AddressLookupConnector] [findById] - Error: ${e.getMessage}")
        Nil
      }
    }
  }

}
