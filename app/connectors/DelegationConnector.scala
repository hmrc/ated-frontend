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

import config.WSHttp
import connectors.AtedConnector.baseUrl
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DelegationConnector {

  def http: CoreGet with CorePost with CoreDelete

  def delegationDataCall(id: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val serviceURL = baseUrl("delegation")
    val jsonData = Json.parse(s"""{"internalId" : "$id"}""".stripMargin)

    val postUrl = s"""$serviceURL/oid"""
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }
}

object DelegationConnector extends DelegationConnector {
  val http = WSHttp
}
