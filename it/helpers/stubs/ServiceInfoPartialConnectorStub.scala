/*
 * Copyright 2022 HM Revenue & Customs
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

package helpers.stubs

import com.github.tomakehurst.wiremock.client.WireMock._

object ServiceInfoPartialConnectorStub {

  def withResponseForNavLinks()(status: Int, optBody: Option[String]): Unit =
    stubFor(get(urlMatching("/business-account/partial/nav-links")) willReturn {
      val coreResponse = aResponse().withStatus(status)
      optBody match {
        case Some(body) => coreResponse.withBody(body)
        case _          => coreResponse
      }
    })

  def verifyNavlinksContent(count: Int): Unit =
    verify(count, getRequestedFor(urlMatching("/business-account/partial/nav-links")))
}