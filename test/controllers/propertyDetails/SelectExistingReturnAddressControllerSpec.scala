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

package controllers.propertyDetails

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
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
import services.{AddressLookupService, FormBundleReturnsService, PropertyDetailsService, SummaryReturnsService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future

class SelectExistingReturnAddressControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val mockPropertyDetailsService = mock[PropertyDetailsService]
  val mockAddressLookupService = mock[AddressLookupService]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockFormBundleReturnsService = mock[FormBundleReturnsService]
  val mockSummaryReturnsService = mock[SummaryReturnsService]
  val returnTypeCharge = "CR"
  val returnTypeRelief = "RR"

  object TestSelectExistingReturnAddressController extends SelectExistingReturnAddressController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val propertyDetailsService = mockPropertyDetailsService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
    override val formBundleReturnService = mockFormBundleReturnsService
    override val summaryReturnService: SummaryReturnsService = mockSummaryReturnsService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockPropertyDetailsService)
    reset(mockDelegationConnector)
    reset(mockAddressLookupService)
    reset(mockBackLinkCache)
  }


  "SelectExistingReturnAddressController" must {

    val prevReturns = Some(Seq(PreviousReturns("1, addressLine1", "12345678")))

    "use correct DelegationConnector" in {
      SelectExistingReturnAddressController.delegationConnector must be(FrontendDelegationConnector)
    }

    "view" must {

      "not respond with NOT_FOUND when we try to view an id" in {
        val result = route(FakeRequest(GET, "/ated/liability/address-lookup/view/2015"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
      }

      "unauthorised users" must {

        "respond with a redirect" in {
          viewWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the address details view if address list is retrieved from cache" in {
          val prevReturns = Seq(PreviousReturns("1, addressLine1", "12345678"))
          viewWithAuthorisedUser(Some(prevReturns)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Select the previous return this new return relates to")
          }
        }

        "show the address details view with no addresses if address list is not retrieved from cache" in {
          viewWithAuthorisedUser(None) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Select the previous return this new return relates to")
          }
        }

      }
    }

    "save" must {

      "not respond with NOT_FOUND when we try to view an id" in {
        val result = route(FakeRequest(POST, "/ated/liability/address-lookup/save/2015"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
      }

      "unauthorised users" must {

        "respond with a redirect" in {
          saveWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }


      "submitting an invalid request should fail and return to the search results page" in {
        saveWithAuthorisedUser(None, prevReturns, Json.toJson(AddressSelected(None))) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Select the previous return this new return relates to")
        }
      }

      "submitting an invalid request should fail and return to the search results page even with cached data" in {
        saveWithAuthorisedUser(None, prevReturns, Json.toJson(AddressSelected(None))) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Select the previous return this new return relates to")

        }
      }

      "submitting an invalid request should fail and return to the search results page even with no cached data" in {
        saveWithAuthorisedUser(None, None, Json.toJson(AddressSelected(None))) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Select the previous return this new return relates to")

        }
      }

      "submitting an valid request should get the form bundle return and save in keystore" in {
        val formBundleProp = FormBundleProperty(BigDecimal(100), new LocalDate("2015-09-08"), new LocalDate("2015-10-12"), "Relief", Some("Property developers"))
        val formBundleProp2 = FormBundleProperty(BigDecimal(200), new LocalDate("2015-10-12"), new LocalDate("2015-12-12"), "Relief", Some("Property developers"))
        val formBundleAddress = FormBundleAddress("1 addressLine1", "addressLine2", Some("addressLine3"), Some("AddressLine4"), Some("XX11XX"), "GB")
        val formBundlePropertyDetails = FormBundlePropertyDetails(Some("title here"), formBundleAddress, Some("additional details"))
        val viewReturn = FormBundleReturn("2014", formBundlePropertyDetails, Some(new LocalDate("2013-10-10")), Some(BigDecimal(100)), Some("ABCdefgh"), Some("PromABCdefgh"), Some("1234"), true, true, new LocalDate("2015-05-10"), BigDecimal(9324), "1234567891", List(formBundleProp))

        saveWithAuthorisedUser(Some(viewReturn), prevReturns, Json.toJson(AddressSelected(Some("12345678")))) {
          result =>
            status(result) must be(SEE_OTHER)
        }
      }

      "submitting an invalid form bundle number request should redirect to Account Summary Page" in {
        saveWithAuthorisedUser(formBundleReturn = None, prevReturns, Json.toJson(AddressSelected(Some("12345678")))) {
          result =>
            status(result) must be(SEE_OTHER)
        }
      }
    }
  }

  def viewWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val prevReturns = Seq(PreviousReturns("1, addressLine1", "12345678"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockSummaryReturnsService.getPreviousSubmittedLiabilityDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(prevReturns))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestSelectExistingReturnAddressController.view(2015, returnTypeCharge).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedUser(prevReturns: Option[Seq[PreviousReturns]])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockSummaryReturnsService.retrieveCachedPreviousReturnAddressList(Matchers.any(), Matchers.any())).thenReturn(Future.successful(prevReturns))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestSelectExistingReturnAddressController.view(2015, returnTypeCharge).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val prevReturns = Some(Seq(PreviousReturns("1, addressLine1", "12345678")))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockSummaryReturnsService.retrieveCachedPreviousReturnAddressList(Matchers.any(), Matchers.any())).thenReturn(Future.successful(prevReturns))
    val result = TestSelectExistingReturnAddressController.continue(2015, returnTypeCharge).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithAuthorisedUser(formBundleReturn: Option[FormBundleReturn],
                             prevReturns: Option[Seq[PreviousReturns]],
                             inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockSummaryReturnsService.retrieveCachedPreviousReturnAddressList(Matchers.any(), Matchers.any())).thenReturn(Future.successful(prevReturns))
    when(mockFormBundleReturnsService.getFormBundleReturns(Matchers.eq("12345678"))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(formBundleReturn))
    when(mockDataCacheConnector.saveFormData[Boolean](Matchers.eq(AtedConstants.SelectedPreviousReturn), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(true))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestSelectExistingReturnAddressController.continue(2014, returnTypeCharge).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }
 }
