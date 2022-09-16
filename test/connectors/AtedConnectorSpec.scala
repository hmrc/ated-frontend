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

package connectors

import java.util.UUID

import builders._
import config.ApplicationConfig
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import testhelpers.MockAuthUtil
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.http.{UnauthorizedException, _}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.AtedConstants

import scala.concurrent.Future

class AtedConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  class Setup {
    val testAtedConnector: AtedConnector = new AtedConnector(mockAppConfig, mockHttp)
  }

  val periodKey: String = "2015"

  override def beforeEach: Unit = {
    reset(mockHttp)
  }

  "AtedConnector" must {

    val periodKey = 2015

    "save reliefs" must {
      "for successful save, return Reliefs for a user" in new Setup {
        val reliefData: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey)

        val successResponse: JsValue = Json.toJson(reliefData)

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))


        val result: Future[HttpResponse] = testAtedConnector.saveDraftReliefs("ATED-123", reliefData)
        await(result).status must be(OK)
      }
      "for successful save, return Reliefs for an agent" in new Setup {
        val reliefData: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey)

        val successResponse: JsValue = Json.toJson(reliefData)

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))


        val result: Future[HttpResponse] = testAtedConnector.saveDraftReliefs("ATED-123", reliefData)
        await(result).status must be(OK)
      }
    }

    "retrieve the Reliefs for a user" in new Setup {
      val reliefData: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey)

      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockHttp.GET[Option[ReliefsTaxAvoidance]]
        (any(),any(),any())
        (any(), any(), any()))
        .thenReturn(Future.successful(Some(reliefData)))

      val result: Future[HttpResponse] = testAtedConnector.retrievePeriodDraftReliefs("ATED-123", periodKey)
      await(result) must be(Some(reliefData))
    }

    "retrieve the Reliefs for an agent" in new Setup {
      val reliefData: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey)

      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockHttp.GET[Option[ReliefsTaxAvoidance]]
        (any(),any(),any())
        (any(), any(), any()))
        .thenReturn(Future.successful(Some(reliefData)))

      val result: Future[HttpResponse] = testAtedConnector.retrievePeriodDraftReliefs("ATED-123", periodKey)
      await(result) must be(Some(reliefData))
    }

    "submit the Reliefs for a user" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockHttp.GET[HttpResponse]
        (any(), any(), any())
        (any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val result: Future[HttpResponse] = testAtedConnector.submitDraftReliefs("ATED-123", periodKey)
      await(result).status must be(OK)
    }

    "submit the Reliefs for an agent" in new Setup {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockHttp.GET[HttpResponse]
        (any(), any(), any())
        (any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val result: Future[HttpResponse] = testAtedConnector.submitDraftReliefs("ATED-123", periodKey)
      await(result).status must be(OK)
    }

    "getDetails" must {
      "GET agent details from ETMP for a user" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse](any(),any(),any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val result: Future[HttpResponse] = testAtedConnector.getDetails("AARN1234567", AtedConstants.IdentifierArn)
        await(result).status must be(OK)
        verify(mockHttp, times(1)).GET[HttpResponse](any(),any(),any())(any(), any(), any())
      }

      "GET user details from ETMP for an agent" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse](any(),any(),any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        val result: Future[HttpResponse] = testAtedConnector.getDetails("XN1200000100001", AtedConstants.IdentifierSafeId)
        await(result).status must be(OK)
        verify(mockHttp, times(1)).GET[HttpResponse](any(),any(),any())(any(), any(), any())
      }
    }

    "Return subscription data" must {
      "get the subscription data" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse: JsValue = Json.parse( """{}""")
        when(mockHttp.GET[HttpResponse](any(), any(), any())
        (any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[HttpResponse] = testAtedConnector.retrieveSubscriptionData()
        await(result).json must be(successResponse)
      }

      "throw 401 unauthorized in case bad retrieval of subscription data" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse]
          (any(), any(), any())
        (any(), any(), any()))
          .thenReturn(Future.failed(new UnauthorizedException("User does not have the correct authorisation")))

        val result: UnauthorizedException = intercept[UnauthorizedException]{await(testAtedConnector.retrieveSubscriptionData())}
        result.message must be("User does not have the correct authorisation")
      }

    }

    "Update subscription data" must {
      val addressDetails: AddressDetails = AddressDetails("Correspondence", "line1", "line2", None, None, Some("postCode"), "GB")
      val updatedData: UpdateSubscriptionDataRequest = UpdateSubscriptionDataRequest(emailConsent = true,
        ChangeIndicators(), List(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)))

      "update the subscription data" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse: JsValue = Json.parse( """{}""")
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))


        val result: Future[HttpResponse] = testAtedConnector.updateSubscriptionData(updatedData)
        await(result).json must be(successResponse)
      }
    }

    "Update registration details" must {
      val updateDetails: UpdateRegistrationDetailsRequest = RegistrationBuilder.getEtmpRegistrationUpdateRequest("oldName")

      "update the registration details" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse: JsValue = Json.parse( """{}""")
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))


        val result: Future[HttpResponse] = testAtedConnector.updateRegistrationDetails("SAFE_123", updateDetails)
        await(result).json must be(successResponse)
      }
    }

    "Return form bundle" must {
      "view return" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse: JsValue = Json.parse( """{}""")
        when(mockHttp.GET[HttpResponse]
          (any(), any(), any())
        (any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[HttpResponse] = testAtedConnector.retrieveFormBundleReturns("formbundle123456")
        await(result).json must be(successResponse)
      }
    }


    "retrieveAndCacheLiabilityReturn" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val result: Future[HttpResponse] = testAtedConnector.retrieveAndCacheLiabilityReturn("1")
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "retrieveAndCachePreviousLiabilityReturn" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val result: Future[HttpResponse] = {
          testAtedConnector.retrieveAndCachePreviousLiabilityReturn("1", periodKey)
        }
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "cacheDraftChangeLiabilityReturnHasBankDetails" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val result: Future[HttpResponse] = {
          testAtedConnector.cacheDraftChangeLiabilityReturnHasBank("1", hasBankDetails = true)
        }
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }


    "cacheDraftChangeLiabilityReturnBank" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val bankDetails = BankDetails()
        val result: Future[HttpResponse] = testAtedConnector.cacheDraftChangeLiabilityReturnBank("1", bankDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "calculateDraftDisposal" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse: JsValue = Json.parse( """{}""")
        when(mockHttp.GET[HttpResponse]
          (any(), any(), any())
        (any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[HttpResponse] = testAtedConnector.calculateDraftDisposal("1")
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }


    "submitDraftChangeLiabilityReturn" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val result: Future[HttpResponse] = testAtedConnector.submitDraftChangeLiabilityReturn("1")
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "retrieveAndCacheDisposeLiability" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val result: Future[HttpResponse] = testAtedConnector.retrieveAndCacheDisposeLiability("1")
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "cacheDraftDisposeLiabilityReturnDate" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val updatedDate: DisposeLiability = DisposeLiabilityReturnBuilder.generateDisposalDate(periodKey)
        val result: Future[HttpResponse] = testAtedConnector.cacheDraftDisposeLiabilityReturnDate("1", updatedDate)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "cacheDraftDisposeLiabilityReturnHasBank" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val result: Future[HttpResponse] = testAtedConnector.cacheDraftDisposeLiabilityReturnHasBank("1", hasBankDetails = true)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }


    "cacheDraftDisposeLiabilityReturnBank" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val bankDetails = BankDetails()
        val result: Future[HttpResponse] = testAtedConnector.cacheDraftDisposeLiabilityReturnBank("1", bankDetails)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "submitDraftDisposeLiabilityReturn" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.POST[JsValue, HttpResponse]
          (any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val result: Future[HttpResponse] = testAtedConnector.submitDraftDisposeLiabilityReturn("1")
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "getFullSummaryReturns" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val result: Future[HttpResponse] = testAtedConnector.getFullSummaryReturns
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "getPartialSummaryReturns" must {
      "return HttpResponse" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))
        val result: Future[HttpResponse] = testAtedConnector.getPartialSummaryReturns
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }
    }

    "delete draft relief return" must {
      "for successful submit, return submit response" in new Setup {
        val successResponse: JsValue = Json.toJson(Seq(ReliefBuilder.reliefTaxAvoidance(periodKey)))
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.DELETE[HttpResponse](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))
        val result: Future[HttpResponse] = testAtedConnector.deleteDraftReliefsByYear(2017)
        val response: HttpResponse = await(result)
        response.status must be(OK)
      }

      "for an invalid id, return an empty object" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockHttp.DELETE[HttpResponse](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))
        val result: Future[HttpResponse] = testAtedConnector.deleteDraftReliefsByYear(4012)
        val response: HttpResponse = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

  }
}
