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

package services

import connectors.ServiceInfoPartialConnector
import controllers.ControllerBaseSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.JsValue
import play.api.mvc.Request
import play.twirl.api.{Html, HtmlFormat}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.{ExecutionContext, Future}

class ServiceInfoServiceSpec extends ControllerBaseSpec {

  val mockConnector: ServiceInfoPartialConnector = mock[ServiceInfoPartialConnector]
  val service: ServiceInfoService = new ServiceInfoService(mockConnector)
  val validHtml: Html = Html("<nav>btalink<nav>")
  val htmlError: Html = Html("error")

  "getServiceInfo Partial" should {
    "return bta Partial" in {
      when(mockConnector.getServiceInfoPartial()(any(), any())).thenReturn(Future.successful(validHtml))

      val result: Html = await(service.getPartial(fakeRequest, organisationStandardRetrievals, ec))
      val expectedResult: Html = validHtml

      result mustBe expectedResult
    }
    "return error HTML for an agent" in {
      when(mockConnector.getServiceInfoPartial()(any(),any())).thenReturn(Future.successful(htmlError))

      val result: Html = await(service.getPartial(fakeRequest, agentStandardRetrievals, ec))
      val expectedResult: Html = HtmlFormat.empty

      result mustBe expectedResult
    }
  }

}
