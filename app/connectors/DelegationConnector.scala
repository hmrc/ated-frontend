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
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.StringContextOps
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._

class DelegationConnector @Inject()(http: HttpClientV2, appConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  val serviceURL: String = appConfig.conf.baseUrl("delegation")

  def delegationDataCall(id: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val jsonData = Json.parse(s"""{"internalId" : "$id"}""".stripMargin)
    val postUrl = url"""$serviceURL/oid"""

    http.post(postUrl).withBody(jsonData).execute[HttpResponse]
  }
}
