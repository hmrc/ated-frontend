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

package connectors

import java.net.URLEncoder

import config.WSHttp
import models.{AddressLookup, AddressLookupRecord, AtedContext}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object AddressLookupConnector extends AddressLookupConnector {
  val serviceURL = baseUrl("address-lookup")
}

trait AddressLookupConnector extends ServicesConfig with RawResponseReads {

  def serviceURL: String
  private val BASE_URL = "/v2/uk/addresses"
  private val POSTCODE_LOOKUP = s"$BASE_URL?postcode="
  private val ID_LOOKUP = s"$BASE_URL/"

  val http: CoreGet with CorePost with CoreDelete = WSHttp

  def findByPostcode(addressLookup: AddressLookup)
                    (implicit atedContext: AtedContext, hc: HeaderCarrier):Future[List[AddressLookupRecord]] = {
    val filter = addressLookup.houseName.map(fi => "&filter=" + enc(fi)).getOrElse("")
    http.GET[List[AddressLookupRecord]](serviceURL + POSTCODE_LOOKUP + addressLookup.postcode + filter).recover {
      case e => Nil
    }
  }

  def findById(id: String)
                    (implicit atedContext: AtedContext, hc: HeaderCarrier):Future[Option[AddressLookupRecord]] = {
    http.GET[Option[AddressLookupRecord]](serviceURL + ID_LOOKUP + enc(id)).recover {
      case e: NotFoundException => None
    }
  }

  private def enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
