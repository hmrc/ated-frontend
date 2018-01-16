/*
 * Copyright 2018 HM Revenue & Customs
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
import services.{ChangeLiabilityReturnService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future

class PropertyDetailsAddressControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockService = mock[PropertyDetailsService]
  val mockChangeService = mock[ChangeLiabilityReturnService]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestPropertyDetailsController extends PropertyDetailsAddressController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val propertyDetailsService = mockService
    override val changeLiabilityReturnService = mockChangeService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockService)
    reset(mockDelegationConnector)
    reset(mockBackLinkCache)
  }


  "PropertyDetailsAddressController" must {

    "use correct DelegationConnector" in {
      PropertyDetailsAddressController.delegationConnector must be(FrontendDelegationConnector)
    }

    "propertyDetails" must {

      "not respond with NOT_FOUND when we try to view an id" in {
        val result = route(FakeRequest(GET, "/ated/liability/create/address/view/1"))
        result.isDefined must be(true)
        status(result.get) must not be(NOT_FOUND)
      }

      "not respond with NOT_FOUND when we try to create" in {
        val result = route(FakeRequest(GET, "/ated/liability/create/address/2015"))
        result.isDefined must be(true)
        status(result.get) must not be(NOT_FOUND)
      }

      "unauthorised users" must {

        "respond with a redirect" in {
          createWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
          createWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the chargeable property details view if we have no id" in {
          createWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Enter the address of the property manually"))

              document.getElementById("line_1_field").attr("value") must be("")
              document.getElementById("submit").text() must be("Save and continue")
          }
        }

        "show the chargeable property details view if we id and data" in {
          viewDataWithAuthorisedUser("1", PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Enter the address of the property manually"))

              document.getElementById("line_1").attr("value") must be("addr1")
              document.getElementById("line_2").attr("value") must be("addr2")
              document.getElementById("line_3").attr("value") must be("addr3")
              document.getElementById("line_4").attr("value") must be("addr4")
              document.getElementById("postcode").attr("value") must be("postCode")
          }
        }
      }
    }


    "view submitted" must {
      "show the details of a submitted return" in {
        viewSubmittedWithAuthorisedUser("1", Some(PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")))) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Enter the address of the property manually"))

            document.getElementById("line_1").attr("value") must be("addr1")
            document.getElementById("line_2").attr("value") must be("addr2")
            document.getElementById("line_3").attr("value") must be("addr3")
            document.getElementById("line_4").attr("value") must be("addr4")
            document.getElementById("postcode").attr("value") must be("postCode")
        }
      }

      "Return to the account summary if we have no details" in {
        viewSubmittedWithAuthorisedUser("1", None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/account-summary")
        }
      }
    }

    "edit from summary" must {
      "show the details of a submitted return with a back link" in {
        editFromSummary("1", PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Enter the address of the property manually"))

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/liability/create/summary")
        }
      }
    }

    "addressLookupRedirect" must {
      "redirect to the address lookup page" in {
        addressLookupRedirect(Some("1")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/address-lookup/view/2015?propertyKey=1")
        }
      }

      "redirect to the address lookup page if we have no id" in {
        addressLookupRedirect(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/liability/address-lookup/view/2015")
        }
      }
    }
    "save" must {
      "unauthorised users" must {

        "be redirected to the login page" in {
          saveWithUnAuthorisedUser(None) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "for invalid data, return BAD_REQUEST" in {

          val inputJson = Json.parse( """{"rentalBusiness": true, "isAvoidanceScheme": "true"}""")
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(None, inputJson) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for valid data with no id, return OK" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsAddress(postCode = Some("XX1 1XX"))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(None, Json.toJson(propertyDetails)) {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }

        "for valid data, return OK" in {
          val propertyDetails = PropertyDetailsBuilder.getPropertyDetailsAddress(postCode = Some("XX1 1XX"))
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Some("1"),Json.toJson(propertyDetails)) {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }
      }
    }
  }


  def createWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsController.createNewDraft(2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def createWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestPropertyDetailsController.createNewDraft(2015).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def createWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockDataCacheConnector.saveFormData[Boolean](Matchers.eq(AtedConstants.SelectedPreviousReturn), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(true))
    val result = TestPropertyDetailsController.createNewDraft(2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def viewDataWithAuthorisedUser(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestPropertyDetailsController.view(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewSubmittedWithAuthorisedUser(id: String, propertyDetails: Option[PropertyDetails])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockChangeService.retrieveSubmittedLiabilityReturnAndCache(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(propertyDetails))
    val result = TestPropertyDetailsController.editSubmittedReturn(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def editFromSummary(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = TestPropertyDetailsController.editFromSummary(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def addressLookupRedirect(id: Option[String])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
     val result = TestPropertyDetailsController.addressLookupRedirect(id, 2015, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def createAuthorisedUserNone(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestPropertyDetailsController.createNewDraft(2015).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def saveWithUnAuthorisedUser(id: Option[String])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsController.save(id, 2015, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithUnAuthenticated(id: Option[String])(test: Future[Result] => Any) {
    val result = TestPropertyDetailsController.save(id, 2015, None).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUser(id: Option[String], inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    id match {
      case Some(x) =>
        when(mockService.saveDraftPropertyDetailsAddress(Matchers.eq(x), Matchers.any())(Matchers.any(), Matchers.any())).
          thenReturn(Future.successful(x))
      case None =>
        when(mockService.createDraftPropertyDetailsAddress(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).
          thenReturn(Future.successful("1"))
    }
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsController.save(id, 2015, None).apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))

    test(result)
  }
}
