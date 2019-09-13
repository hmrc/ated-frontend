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

package controllers

import java.util.UUID

import builders.{SessionBuilder, TitleBuilder}
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{PreviousReturns, ReturnType}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DelegationService, SummaryReturnsService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class ReturnTypeControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  val periodKey: Int = 2015
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockSummaryReturnsService: SummaryReturnsService = mock[SummaryReturnsService]

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  object TestReturnTypeController extends ReturnTypeController {
    override val summaryReturnService: SummaryReturnsService = mockSummaryReturnsService
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = DelegationService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBackLinkCache)
  }

  "ReturnTypeController" must {

    "use correct DelegationService" in {
      ReturnTypeController.delegationService must be(DelegationService)
    }

    "returnType" must {

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }

        }
      }

      "Authorised users" must {
        "show the return type view in version 2" in {
          getWithAuthorisedUser { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select a type of return"))
            document.getElementById("return-type-header").text() must be("Select a type of return")
            document.getElementById("returnType-rr_field").text() must be("For a property or properties in relief where no ATED charge is due")
          }
        }
        "show the return type view with saved data" in {
          getWithAuthorisedUserWithSomeData { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select a type of return"))
            document.getElementById("return-type-header").text() must be("Select a type of return")
            document.getElementById("returnType-rr_field").text() must be("For a property or properties in relief where no ATED charge is due")
            document.getElementById("returnType-rr").attr("checked") must be("checked")
          }
        }

      }
    }

    "submit" must {
      "for authorised user" must {
        val prevReturns = Seq(PreviousReturns("1, addressLine1", "12345678"))
        "with valid form data" must {
          "with invalid form, return BadRequest" in {
            val inputJson = Json.parse( """{"returnType": ""}""")
            when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
            submitWithAuthorisedUser(prevReturns, FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                val doc = Jsoup.parse(contentAsString(result))
                doc.getElementsByClass("error-notification").html() must include("Select an option for type of return")
                contentAsString(result) must include("Select an option for type of return")
            }
          }
          "with returnType=RR - relief return Redirect to choose relief page" in {
            val inputJson = Json.parse( """{"returnType": "RR"}""")
            submitWithAuthorisedUser(prevReturns, FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must be("/ated/reliefs/2015/choose")
            }
          }
          "with returnType=CR - chargeable return, status is OK" in {
            val inputJson = Json.parse( """{"returnType": "CR"}""")
            submitWithAuthorisedUser(prevReturns, FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must be("/ated/existing-return/confirmation/2015/charge")
            }
          }

          "with returnType=CR - chargeable return and previous submitted details found from ETMP, status is OK" in {
            val inputJson = Json.parse( """{"returnType": "CR"}""")
            submitWithAuthorisedUser(Nil, FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must be("/ated/liability/address-lookup/view/2015")
            }
          }
        }
        "with returnType=anything else, status is Redirect" in {
          val inputJson = Json.parse( """{"returnType": "INVALID"}""")
          submitWithAuthorisedUser(prevReturns, FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must be("/ated/return-type/2015")
          }
        }
      }
    }


  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[String](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestReturnTypeController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserWithSomeData(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[ReturnType](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ReturnType(Some("RR")))))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestReturnTypeController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestReturnTypeController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(prevReturns: Seq[PreviousReturns], fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockSummaryReturnsService.getPreviousSubmittedLiabilityDetails(Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(prevReturns))
    when(mockBackLinkCache.clearBackLinks(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Nil))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestReturnTypeController.submit(periodKey).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }
}
