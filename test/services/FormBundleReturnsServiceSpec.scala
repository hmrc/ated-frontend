/*
 * Copyright 2017 HM Revenue & Customs
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

import builders.AuthBuilder
import connectors.AtedConnector
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.SessionId

import scala.concurrent.Future


class FormBundleReturnsServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockConnector = mock[AtedConnector]

  object TestFormBundleReturnsService extends FormBundleReturnsService {
    override val atedConnector = mockConnector

  }

  val successJson =
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


  override def beforeEach = {
    reset(mockConnector)
  }

  implicit val user = createAtedContext(createAgentAuthContext("User-Id", "name", Some("JARN1234567")))

  "FormBundleReturnsService" must {
    "use the correct connectors" in {
      FormBundleReturnsService.atedConnector must be(AtedConnector)
    }

    "return data if we have some" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val successResponse = Json.parse(successJson)

      when(mockConnector.retrieveFormBundleReturns(Matchers.eq("12345678901090"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
      val result = TestFormBundleReturnsService.getFormBundleReturns("12345678901090")
      val bundleReturn = await(result)
      bundleReturn.isDefined must be(true)

      bundleReturn.get.periodKey must be("2014")
      bundleReturn.get.propertyDetails.titleNumber must be(Some("title here"))
      bundleReturn.get.propertyDetails.additionalDetails must be(Some("additional additional"))
      bundleReturn.get.dateOfAcquisition must be(Some(new LocalDate("2013-10-10")))
      bundleReturn.get.ninetyDayRuleApplies must be(true)

      bundleReturn.get.lineItem.size must be(1)

    }


    "return no data if we have none" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val notFoundResponse = Json.parse( """{}""")

      when(mockConnector.retrieveFormBundleReturns(Matchers.eq("12345678901090"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundResponse))))
      val result = TestFormBundleReturnsService.getFormBundleReturns("12345678901090")
      await(result).isDefined must be(false)
    }

    "throws an exception for a bad request" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))
      when(mockConnector.retrieveFormBundleReturns(Matchers.eq("12345678901090"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))

      val result = TestFormBundleReturnsService.getFormBundleReturns("12345678901090")
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Failed to retrieve form bundle return")
    }

    "throws an exception for a unknown exception" in {
      implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))
      when(mockConnector.retrieveFormBundleReturns(Matchers.eq("12345678901090"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(UNAUTHORIZED, None)))

      val result = TestFormBundleReturnsService.getFormBundleReturns("12345678901090")
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("Failed to retrieve form bundle return")
    }


  }
}
