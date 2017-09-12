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

import builders.{AuthBuilder, PropertyDetailsBuilder, SessionBuilder, TestAudit}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models._
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
import services.{AddressLookupService, ChangeLiabilityReturnService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants
import utils.AtedConstants.SubmitReturnsResponseFormId

import scala.concurrent.Future

class AddressLookupControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockPropertyDetailsService = mock[PropertyDetailsService]
  val mockAddressLookupService = mock[AddressLookupService]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestAddressLookupController extends AddressLookupController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val propertyDetailsService = mockPropertyDetailsService
    override val addressLookupService = mockAddressLookupService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockPropertyDetailsService)
    reset(mockDelegationConnector)
    reset(mockAddressLookupService)
    reset(mockBackLinkCache)
  }


  "AddressLookupController" must {

    val address1 = AddressLookupRecord("1", AddressSearchResult(List("1", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))
    val address2 = AddressLookupRecord("2", AddressSearchResult(List("2", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))
    val address3 = AddressLookupRecord("3", AddressSearchResult(List("3", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))

    "use correct DelegationConnector" in {
      AddressLookupController.delegationConnector must be(FrontendDelegationConnector)
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

        "show the chargeable property details view if we have no id" in {
          viewWithAuthorisedUser(None) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Find the property's address")
          }
        }
      }
    }
    "find" must {

      "not respond with NOT_FOUND when we try to view an id" in {
        val result = route(FakeRequest(POST, "/ated/liability/address-lookup/find/2015"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
      }

      "unauthorised users" must {

        "respond with a redirect" in {
          findWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "submitting an invalid request should fail and return to the search page" in {
        val searchCriteria = AddressLookup("", None)
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        findWithAuthorisedUser(None, Json.toJson(searchCriteria), AddressSearchResults(searchCriteria, Nil)) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Find the property's address")

        }
      }


      "submitting a valid request should that returns no search results should show this on the screen" in {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        findWithAuthorisedUser(None, Json.toJson(searchCriteria), AddressSearchResults(searchCriteria, Nil)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Select the address of the property")

            document.getElementById("no-address-found").text() must be("No addresses were found for this postcode")
        }
      }

      "submitting a valid request should lookup search results and return them to the screen" in {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        val results = List(address1, address2, address3)
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        findWithAuthorisedUser(None, Json.toJson(searchCriteria), AddressSearchResults(searchCriteria, results)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Select the address of the property")

            document.getElementById("no-address-found") must be(null)
        }
      }
    }

    "manualAddressRedirect" must {

      "redirect to the manual address page" in {
        manualAddressRedirect(Some("1")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/address/view/1")

        }
      }

      "redirect to the manual address page when we have no id" in {
        manualAddressRedirect(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/address/2015")

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
        val searchCriteria = AddressLookup("XX1 1XX", None)
        val searchResults =  AddressSearchResults(searchCriteria, Nil)
        saveWithAuthorisedUser(None, 2015, Json.toJson(AddressSelected(None)), Some(searchResults), None) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Select the address of the property")

        }
      }

      "submitting an invalid request should fail and return to the search results page even with no cached data" in {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        val searchResults =  AddressSearchResults(searchCriteria, Nil)
        saveWithAuthorisedUser(None, 2015, Json.toJson(AddressSelected(None)), None, None) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Select the address of the property")

        }
      }

      "submitting a valid request should fail if we don't find the property" in {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        val results = AddressSearchResults(searchCriteria,List(address1, address2, address3))

        saveWithAuthorisedUser(None, 2015, Json.toJson(AddressSelected(Some("1"))), Some(results), None) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Select the address of the property")

        }
      }

      "submitting a valid request should fail if we don't find the property and hae no cached data" in {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        val results = AddressSearchResults(searchCriteria,List(address1, address2, address3))

        saveWithAuthorisedUser(None, 2015, Json.toJson(AddressSelected(Some("1"))), None, None) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Select the address of the property")

        }
      }


      "submitting a valid request should create a new return if we have no Id" in {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        val foundProperty = PropertyDetailsAddress("", "", None, None, None)
        when(mockPropertyDetailsService.createDraftPropertyDetailsAddress(Matchers.eq(2015), Matchers.eq(foundProperty))(Matchers.any(), Matchers.any())).thenReturn(Future.successful("newId"))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

        saveWithAuthorisedUser(None, 2015, Json.toJson(AddressSelected(Some("1"))), None, Some(foundProperty)) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/title/view/newId")
        }
      }
      "submitting a valid request should update a return if we have an Id" in {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        val foundProperty = PropertyDetailsAddress("", "", None, None, None)
        when(mockPropertyDetailsService.saveDraftPropertyDetailsAddress(Matchers.any(), Matchers.eq(foundProperty))(Matchers.any(), Matchers.any())).thenReturn(Future.successful("1"))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

        saveWithAuthorisedUser(Some("1"), 2015, Json.toJson(AddressSelected(Some("1"))), None, Some(foundProperty)) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/title/view/1")

        }
      }
    }
  }

  def viewWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestAddressLookupController.view(None, 2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedUser(id: Option[String])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestAddressLookupController.view(id, 2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def findWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = TestAddressLookupController.find(None, 2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def findWithAuthorisedUser(id: Option[String], inputJson: JsValue, results: AddressSearchResults)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockAddressLookupService.find(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(results))
    val result = TestAddressLookupController.find(id, 2015).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }

  def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = TestAddressLookupController.save(None, 2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithAuthorisedUser(id: Option[String],
                             periodKey: Int,
                             inputJson: JsValue,
                             results: Option[AddressSearchResults],
                             selected: Option[PropertyDetailsAddress])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockAddressLookupService.retrieveCachedSearchResults()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(results))
    when(mockAddressLookupService.findById(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(selected))

    val result = TestAddressLookupController.save(id, periodKey).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }

  def manualAddressRedirect(id: Option[String])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some("http://")))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestAddressLookupController.manualAddressRedirect(id, 2015, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
