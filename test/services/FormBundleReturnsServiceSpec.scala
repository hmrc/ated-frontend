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

package services

import java.util.UUID

import connectors.AtedConnector
import models.{FormBundleReturn, StandardAuthRetrievals}
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, InternalServerException}

import scala.concurrent.Future


class FormBundleReturnsServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockConnector: AtedConnector = mock[AtedConnector]

  class Setup {
    val testFormBundleReturnsService: FormBundleReturnsService = new FormBundleReturnsService(
      mockConnector
    )
  }

  val successJson: String =
    """
      |{
      |  "periodKey": "2014",
      |  "propertyDetails": {
      |    "titleNumber": "title here",
      |    "address": {
      |      "addressLine1": "1 addressLine1",
      |      "addressLine2": "addressLine2",
      |      "postalCode": "XX11XX",
      |      "countryCode": "GB"
      |    },
      |    "additionalDetails": "additional additional"
      |  },
      |  "dateOfAcquisition": "2013-10-10",
      |  "valueAtAcquisition": 100,
      |  "dateOfValuation": "2014-10-10",
      |  "taxAvoidanceScheme": "ABCDefgh",
      |  "localAuthorityCode": "1234",
      |  "professionalValuation": true,
      |  "ninetyDayRuleApplies": true,
      |  "dateOfSubmission": "2016-05-10",
      |  "liabilityAmount": 9375,
      |  "paymentReference": "abc456def123gh",
      |  "lineItem": [
      |    {
      |      "propertyValue": 100,
      |      "dateFrom": "2014-10-10",
      |      "dateTo": "2016-10-10",
      |      "type": "Relief",
      |      "reliefDescription": "Property developers"
      |    }
      |  ]
      |}
    """.stripMargin


  override def beforeEach: Unit = {
    reset(mockConnector)
  }

  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]

  "FormBundleReturnsService" must {
    "return data if we have some" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val successResponse: JsValue = Json.parse(successJson)

      when(mockConnector.retrieveFormBundleReturns(ArgumentMatchers.eq("12345678901090"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
      val result: Future[Option[FormBundleReturn]] = testFormBundleReturnsService.getFormBundleReturns("12345678901090")
      val bundleReturn: Option[FormBundleReturn] = await(result)
      bundleReturn.isDefined must be(true)

      bundleReturn.get.periodKey must be("2014")
      bundleReturn.get.propertyDetails.titleNumber must be(Some("title here"))
      bundleReturn.get.propertyDetails.additionalDetails must be(Some("additional additional"))
      bundleReturn.get.dateOfAcquisition must be(Some(new LocalDate("2013-10-10")))
      bundleReturn.get.ninetyDayRuleApplies must be(true)

      bundleReturn.get.lineItem.size must be(1)
    }

    "return no data if we have none" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val notFoundResponse: JsValue = Json.parse( """{}""")

      when(mockConnector.retrieveFormBundleReturns(ArgumentMatchers.eq("12345678901090"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundResponse))))
      val result: Future[Option[FormBundleReturn]] = testFormBundleReturnsService.getFormBundleReturns("12345678901090")
      await(result).isDefined must be(false)
    }

    "throws an exception for a bad request" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))
      when(mockConnector.retrieveFormBundleReturns(ArgumentMatchers.eq("12345678901090"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

      val result: Future[Option[FormBundleReturn]] = testFormBundleReturnsService.getFormBundleReturns("12345678901090")
      val thrown: BadRequestException = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Failed to retrieve form bundle return")
    }

    "throws an exception for a unknown exception" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))
      when(mockConnector.retrieveFormBundleReturns(ArgumentMatchers.eq("12345678901090"))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNAUTHORIZED, None)))

      val result: Future[Option[FormBundleReturn]] = testFormBundleReturnsService.getFormBundleReturns("12345678901090")
      val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("Failed to retrieve form bundle return")
    }


  }
}
