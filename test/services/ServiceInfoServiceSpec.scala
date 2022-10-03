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

package services

import connectors.ServiceInfoPartialConnector
import controllers.ControllerBaseSpec
import models.requests.{NavContent, NavLinks}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import play.twirl.api.{Html, HtmlFormat}
import views.html.{BtaNavigationLinks, service_info}

import scala.concurrent.Future

class ServiceInfoServiceSpec extends ControllerBaseSpec {

  val mockConnector: ServiceInfoPartialConnector = mock[ServiceInfoPartialConnector]
  val btanl: BtaNavigationLinks = injector.instanceOf[BtaNavigationLinks]
  val serviceInfoView: service_info = injector.instanceOf[service_info]
  val service: ServiceInfoService = new ServiceInfoService(mockConnector,serviceInfoView, btanl)(messagesApi, mockAppConfig)
  val navLinks = NavLinks("en", "/nav", None)
  val navContent = NavContent(navLinks, navLinks, navLinks, navLinks, navLinks)

  "getServiceInfo Partial" should {
    "return bta Partial" in {
      when(mockConnector.getNavLinks(any(), any())).thenReturn(Future.successful(Some(navContent)))

      val result: Html = await(service.getPartial(ec, organisationStandardRetrievals, fakeRequest ))
      val expectedResult: Html = serviceInfoView(utils.PartialFactory.partialList(navContent))

      result mustBe expectedResult
    }
    "return error HTML for an agent" in {

      val result: Html = await(service.getPartial(ec, agentStandardRetrievals, fakeRequest))
      val expectedResult: Html = HtmlFormat.empty

      result mustBe expectedResult
    }
  }

}
