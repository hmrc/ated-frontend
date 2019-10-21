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

package controllers.propertyDetails

import java.util.UUID

import builders._
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models._
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AddressLookupService, PropertyDetailsService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class AddressLookupControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
  val mockAddressLookupService: AddressLookupService = mock[AddressLookupService]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val periodKey: Int = 2015

  val address1 = AddressLookupRecord("1", AddressSearchResult(List("1", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))
  val address2 = AddressLookupRecord("2", AddressSearchResult(List("2", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))
  val address3 = AddressLookupRecord("3", AddressSearchResult(List("3", "result street"), None, None, "XX1 1XX", AddressLookupCountry("UK", "UK")))

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testAddressLookupController: AddressLookupController = new AddressLookupController(
    mockMcc,
    mockAuditConnector,
    mockAddressLookupService,
    mockAuthAction,
    mockBackLinkCacheConnector,
    mockPropertyDetailsService,
    mockDataCacheConnector
  )

  def viewWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = testAddressLookupController.view(None, periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewWithAuthorisedUser(id: Option[String])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = testAddressLookupController.view(id, periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def findWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = testAddressLookupController.find(None, periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def findWithAuthorisedUser(id: Option[String], inputJson: JsValue, results: AddressSearchResults)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockAddressLookupService.find(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(results))
    val result = testAddressLookupController.find(id, periodKey).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }

  def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = testAddressLookupController.save(None, periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithAuthorisedUser(id: Option[String],
                             periodKey: Int,
                             inputJson: JsValue,
                             results: Option[AddressSearchResults],
                             selected: Option[PropertyDetailsAddress])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockAddressLookupService.retrieveCachedSearchResults()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(results))
    when(mockAddressLookupService.findById(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(selected))

    val result = testAddressLookupController.save(id, periodKey).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
    test(result)
  }

  def manualAddressRedirect(id: Option[String])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some("http://")))
    when(mockBackLinkCacheConnector.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = testAddressLookupController.manualAddressRedirect(id, periodKey, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
  override def beforeEach(): Unit = {

    reset(mockAuditConnector)
    reset(mockAddressLookupService)
    reset(mockBackLinkCacheConnector)
    reset(mockPropertyDetailsService)
    reset(mockDelegationService)
    reset(mockDataCacheConnector)
  }

  "AddressLookupController" must {
    "view" must {

      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          viewWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the chargeable property details view if we have no id" in new Setup {
          viewWithAuthorisedUser(None) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Find the property’s address"))
          }
        }
      }
    }
    "find" must {
      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          findWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "submitting an invalid request should fail and return to the search page" in new Setup {
        val searchCriteria = AddressLookup("", None)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        findWithAuthorisedUser(None, Json.toJson(searchCriteria), AddressSearchResults(searchCriteria, Nil)) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Find the property’s address"))

        }
      }

      "submitting a valid request should that returns no search results should show this on the screen" in new Setup {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        findWithAuthorisedUser(None, Json.toJson(searchCriteria), AddressSearchResults(searchCriteria, Nil)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select the address of the property"))

            document.getElementById("no-address-found").text() must be("No addresses were found for this postcode")
        }
      }

      "submitting a valid request should lookup search results and return them to the screen" in new Setup {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        val results = List(address1, address2, address3)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        findWithAuthorisedUser(None, Json.toJson(searchCriteria), AddressSearchResults(searchCriteria, results)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select the address of the property"))

            document.getElementById("no-address-found") must be(null)
        }
      }
    }

    "manualAddressRedirect" must {

      "redirect to the manual address page" in new Setup {
        manualAddressRedirect(Some("1")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/address/view/1")

        }
      }

      "redirect to the manual address page when we have no id" in new Setup {
        manualAddressRedirect(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/address/2015")

        }
      }
    }
    "save" must {
      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          saveWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "submitting an invalid request should fail and return to the search results page" in new Setup {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        val searchResults =  AddressSearchResults(searchCriteria, Nil)
        saveWithAuthorisedUser(None, periodKey, Json.toJson(AddressSelected(None)), Some(searchResults), None) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select the address of the property"))

        }
      }

      "submitting an invalid request should fail and return to the search results page even with no cached data" in new Setup {
        saveWithAuthorisedUser(None, periodKey, Json.toJson(AddressSelected(None)), None, None) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select the address of the property"))

        }
      }

      "submitting a valid request should fail if we don't find the property" in new Setup {
        val searchCriteria = AddressLookup("XX1 1XX", None)
        val results = AddressSearchResults(searchCriteria,List(address1, address2, address3))

        saveWithAuthorisedUser(None, periodKey, Json.toJson(AddressSelected(Some("1"))), Some(results), None) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select the address of the property"))
        }
      }

      "submitting a valid request should fail if we don't find the property and hae no cached data" in new Setup {

        saveWithAuthorisedUser(None, periodKey, Json.toJson(AddressSelected(Some("1"))), None, None) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select the address of the property"))
        }
      }


      "submitting a valid request should create a new return if we have no Id" in new Setup {
        val value = 2015
        val foundProperty = PropertyDetailsAddress("", "", None, None, None)
        when(mockPropertyDetailsService.createDraftPropertyDetailsAddress
        (Matchers.eq(value), Matchers.eq(foundProperty))(Matchers.any(), Matchers.any())).thenReturn(Future.successful("newId"))
        when(mockBackLinkCacheConnector.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(None, periodKey, Json.toJson(AddressSelected(Some("1"))), None, Some(foundProperty)) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/title/view/newId")
        }
      }
      "submitting a valid request should update a return if we have an Id" in new Setup {
        val foundProperty = PropertyDetailsAddress("", "", None, None, None)
        when(mockPropertyDetailsService.saveDraftPropertyDetailsAddress
        (Matchers.any(), Matchers.eq(foundProperty))(Matchers.any(), Matchers.any())).thenReturn(Future.successful("1"))
        when(mockBackLinkCacheConnector.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(Some("1"), periodKey, Json.toJson(AddressSelected(Some("1"))), None, Some(foundProperty)) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/create/title/view/1")

        }
      }
    }
  }
}
