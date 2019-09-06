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

import java.util.UUID

import builders._
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Mode.Mode
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.{Configuration, Play}
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{CoreDelete, UnauthorizedException, _}
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class AtedConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]

  trait MockedVerbs extends CoreGet with CorePost with CoreDelete
  val mockWSHttp: CoreGet with CorePost with CoreDelete = mock[MockedVerbs]

  val periodKey: String = "2015"

  object TestAtedConnector extends AtedConnector {
    override val http: CoreGet with CorePost with CoreDelete = mockWSHttp
    override val serviceURL: String = baseUrl("ated")

    override protected def mode: Mode = Play.current.mode

    override protected def runModeConfiguration: Configuration = Play.current.configuration
  }

  override def beforeEach: Unit = {
    reset(mockWSHttp)
  }

  "AtedConnector" must {

    val periodKey = 2015

    "save reliefs" must {
      "for successful save, return Reliefs for a user" in {
        val reliefData = ReliefBuilder.reliefTaxAvoidance(periodKey)

        val successResponse = Json.toJson(reliefData)

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result = TestAtedConnector.saveDraftReliefs("ATED-123", reliefData)
        await(result).status must be(OK)
      }
      "for successful save, return Reliefs for an agent" in {
        val reliefData = ReliefBuilder.reliefTaxAvoidance(periodKey)

        val successResponse = Json.toJson(reliefData)

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result = TestAtedConnector.saveDraftReliefs("ATED-123", reliefData)
        await(result).status must be(OK)
      }
    }

    "retrieve the Reliefs for a user" in {
      val reliefData = ReliefBuilder.reliefTaxAvoidance(periodKey)

      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.GET[Option[ReliefsTaxAvoidance]]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(reliefData)))

      val result = TestAtedConnector.retrievePeriodDraftReliefs("ATED-123", periodKey)
      await(result) must be(Some(reliefData))
    }

    "retrieve the Reliefs for an agent" in {
      val reliefData = ReliefBuilder.reliefTaxAvoidance(periodKey)

      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.GET[Option[ReliefsTaxAvoidance]]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(reliefData)))

      val result = TestAtedConnector.retrievePeriodDraftReliefs("ATED-123", periodKey)
      await(result) must be(Some(reliefData))
    }

    "submit the Reliefs for a user" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))

      val result = TestAtedConnector.submitDraftReliefs("ATED-123", periodKey)
      await(result).status must be(OK)
    }

    "submit the Reliefs for an agent" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))

      val result = TestAtedConnector.submitDraftReliefs("ATED-123", periodKey)
      await(result).status must be(OK)
    }

    "getDetails" must {
      "GET agent details from ETMP for a user" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val result = TestAtedConnector.getDetails("AARN1234567", AtedConstants.IdentifierArn)
        await(result).status must be(OK)
        verify(mockWSHttp, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      "GET user details from ETMP for an agent" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        val result = TestAtedConnector.getDetails("XN1200000100001", AtedConstants.IdentifierSafeId)
        await(result).status must be(OK)
        verify(mockWSHttp, times(1)).GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }
    }

    "Return subscription data" must {
      "get the subscription data" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse = Json.parse( """{}""")
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.retrieveSubscriptionData()
        await(result).json must be(successResponse)
      }

      "throw 401 unauthorized in case bad retrieval of subscription data" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse = Json.parse( """{}""")
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.failed(new UnauthorizedException("User does not have the correct authorisation")))

        val result = intercept[UnauthorizedException]{await(TestAtedConnector.retrieveSubscriptionData())}
        result.message must be("User does not have the correct authorisation")
      }

    }

    "Update subscription data" must {
      val addressDetails = AddressDetails("Correspondence", "line1", "line2", None, None, Some("postCode"), "GB")
      val updatedData = UpdateSubscriptionDataRequest(emailConsent = true, ChangeIndicators(), List(Address(Some("name1"), Some("name2"), addressDetails = addressDetails)))

      "update the subscription data" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse = Json.parse( """{}""")
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result = TestAtedConnector.updateSubscriptionData(updatedData)
        await(result).json must be(successResponse)
      }
    }

    "Update registration details" must {
      val updateDetails = RegistrationBuilder.getEtmpRegistrationUpdateRequest("oldName")

      "update the registration details" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse = Json.parse( """{}""")
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))


        val result = TestAtedConnector.updateRegistrationDetails("SAFE_123", updateDetails)
        await(result).json must be(successResponse)
      }
    }

    "Return form bundle" must {
      "view return" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse = Json.parse( """{}""")
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.retrieveFormBundleReturns("formbundle123456")
        await(result).json must be(successResponse)
      }
    }


    "retrieveAndCacheLiabilityReturn" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = TestAtedConnector.retrieveAndCacheLiabilityReturn("1")
        val response = await(result)
        response.status must be(OK)
      }
    }

    "retrieveAndCachePreviousLiabilityReturn" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = {
          TestAtedConnector.retrieveAndCachePreviousLiabilityReturn("1", periodKey)
        }
        val response = await(result)
        response.status must be(OK)
      }
    }

    "cacheDraftChangeLiabilityReturnHasBankDetails" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = {
          TestAtedConnector.cacheDraftChangeLiabilityReturnHasBank("1", hasBankDetails = true)
        }
        val response = await(result)
        response.status must be(OK)
      }
    }


    "cacheDraftChangeLiabilityReturnBank" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val bankDetails = BankDetails()
        val result = TestAtedConnector.cacheDraftChangeLiabilityReturnBank("1", bankDetails)
        val response = await(result)
        response.status must be(OK)
      }
    }

    "calculateDraftDisposal" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse = Json.parse( """{}""")
        when(mockWSHttp.GET[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestAtedConnector.calculateDraftDisposal("1")
        val response = await(result)
        response.status must be(OK)
      }
    }


    "submitDraftChangeLiabilityReturn" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = TestAtedConnector.submitDraftChangeLiabilityReturn("1")
        val response = await(result)
        response.status must be(OK)
      }
    }

    "retrieveAndCacheDisposeLiability" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = TestAtedConnector.retrieveAndCacheDisposeLiability("1")
        val response = await(result)
        response.status must be(OK)
      }
    }

    "cacheDraftDisposeLiabilityReturnDate" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val updatedDate = DisposeLiabilityReturnBuilder.generateDisposalDate(periodKey)
        val result = TestAtedConnector.cacheDraftDisposeLiabilityReturnDate("1", updatedDate)
        val response = await(result)
        response.status must be(OK)
      }
    }

    "cacheDraftDisposeLiabilityReturnHasBank" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = TestAtedConnector.cacheDraftDisposeLiabilityReturnHasBank("1", hasBankDetails = true)
        val response = await(result)
        response.status must be(OK)
      }
    }


    "cacheDraftDisposeLiabilityReturnBank" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val bankDetails = BankDetails()
        val result = TestAtedConnector.cacheDraftDisposeLiabilityReturnBank("1", bankDetails)
        val response = await(result)
        response.status must be(OK)
      }
    }

    "submitDraftDisposeLiabilityReturn" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = TestAtedConnector.submitDraftDisposeLiabilityReturn("1")
        val response = await(result)
        response.status must be(OK)
      }
    }

    "getFullSummaryReturns" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = TestAtedConnector.getFullSummaryReturns
        val response = await(result)
        response.status must be(OK)
      }
    }

    "getPartialSummaryReturns" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = TestAtedConnector.getPartialSummaryReturns
        val response = await(result)
        response.status must be(OK)
      }
    }

    "deleteDraftReliefs" must {
      "return HttpResponse" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.DELETE[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = TestAtedConnector.deleteDraftReliefs
        val response = await(result)
        response.status must be(OK)
      }
    }


    "delete draft relief return" must {
      "for successful submit, return submit response" in {
        val successResponse = Json.toJson(Seq(ReliefBuilder.reliefTaxAvoidance(periodKey)))
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.DELETE[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
        val result = TestAtedConnector.deleteDraftReliefsByYear(2017)
        val response = await(result)
        response.status must be(OK)
      }

      "for an inavlid id, return an empty object" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.DELETE[HttpResponse]
          (Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
        val result = TestAtedConnector.deleteDraftReliefsByYear(4012)
        val response = await(result)
        response.status must be(BAD_REQUEST)
      }
    }

  }
}
