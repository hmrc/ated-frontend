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

package controllers.editLiability

import java.util.UUID

import builders._
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DisposeLiabilityReturnService
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class DisposePropertyControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockDisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestDisposePropertyController extends DisposePropertyController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val disposeLiabilityReturnService: DisposeLiabilityReturnService = mockDisposeLiabilityReturnService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockDelegationConnector)
    reset(mockDisposeLiabilityReturnService)
    reset(mockBackLinkCache)
  }

  val oldFormBundleNum = "123456789012"

  "DisposePropertyController" must {

    "use correct DelegationConnector" in {
      DisposePropertyController.delegationConnector must be(FrontendDelegationConnector)
    }

    "use correct Service" in {
      DisposePropertyController.disposeLiabilityReturnService must be(DisposeLiabilityReturnService)
    }


    "view" must {

      "not respond with Not_Found" in {
        val result = route(FakeRequest(GET, "/ated/liability/1234567890/dispose"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
      }

      "return a status of OK, when date of disposal is some(date)" in {
        val disposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("12345678901")
        viewWithAuthorisedUser(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("When did you dispose of the property?")
            document.getElementById("dispose-property-header").text() must be("When did you dispose of the property?")
            document.getElementById("date-of-disposal").text() must be("For example, 31 3 2017 Day Month Year")
            document.getElementById("submit").text() must be("Save and continue")
        }
      }

      "return a status of OK with pre-filled disposeLiabilityForm, when DisposeLiability model is found in the cache" in {
        val fAddress = FormBundleAddress("line1", "line2", None, None, None, "GB")
        val fProperty = FormBundlePropertyDetails(None, fAddress, None)
        val fReturn = FormBundleReturn("2015", fProperty, dateOfAcquisition = None, valueAtAcquisition = None,
          taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false,
          dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq())

        val disposeLiabilityReturn = DisposeLiabilityReturn(id = "12345678901", fReturn, disposeLiability = Some(DisposeLiability(None, 2015)))
        viewWithAuthorisedUser(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val doc = Jsoup.parse(contentAsString(result))
            doc.title() must be("When did you dispose of the property?")
        }
      }

      "return a status of OK with empty form, if DisposeLiability is not found in cache" in {
        val fAddress = FormBundleAddress("line1", "line2", None, None, None, "GB")
        val fProperty = FormBundlePropertyDetails(None, fAddress, None)
        val fReturn = FormBundleReturn("2015", fProperty, dateOfAcquisition = None, valueAtAcquisition = None,
          taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false,
          dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq())

        val disposeLiabilityReturn = DisposeLiabilityReturn(id = "12345678901", fReturn)
        viewWithAuthorisedUser(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in {
        viewWithAuthorisedUser(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "editFromSummary" must {

      "return a status of OK and have the back link set to the summary page" in {
        val disposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("12345678901").copy(bankDetails = Some(BankDetailsModel()))
        editFromSummary(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("When did you dispose of the property?")

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/liability/123456789012/dispose/summary")
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in {
        editFromSummary(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "save" must {

      "for invalid data, return BAD_REQUEST" in {
        val inputJson = Json.parse(
          """{"dateOfDisposal.day": "30", "periodKey": 2014}""".stripMargin)
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0)).cacheDisposeLiabilityReturnDate(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "for valid, redirect to bank details page" in {
        val inputJson = Json.parse(
          """{"dateOfDisposal.day": "30", "dateOfDisposal.month": "6", "dateOfDisposal.year": "2015", "periodKey": 2015}""".stripMargin)
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/123456789012/dispose/has-bank-details"))
            verify(mockDisposeLiabilityReturnService, times(1)).cacheDisposeLiabilityReturnDate(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

    }
  }

  def viewWithAuthorisedUser(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(Matchers.eq(oldFormBundleNum))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestDisposePropertyController.view(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editFromSummary(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(Matchers.eq(oldFormBundleNum))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
    val result = TestDisposePropertyController.editFromSummary(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def saveWithAuthorisedUser(oldFormBundleNum: String, inputJson: JsValue)(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val disposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("123456789012")
    when(mockDisposeLiabilityReturnService.cacheDisposeLiabilityReturnDate(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(disposeLiabilityReturn)))
    val result = TestDisposePropertyController.save(oldFormBundleNum).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }

}
