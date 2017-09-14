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

import builders.{AuthBuilder, PropertyDetailsBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{AtedContext, SubmitReturnsResponse}
import org.joda.time.DateTime
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
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.frontend.auth.DummyDelegationData
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, UserId}
import utils.AtedConstants

import scala.concurrent.Future

class PropertyDetailsDeclarationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockService = mock[PropertyDetailsService]
  val mockDelegationConnector = mock[DelegationConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]


  implicit def atedContext2AuthContext(implicit atedContext: AtedContext) = atedContext.user.authContext

  object TestPropertyDetailsDeclarationController extends PropertyDetailsDeclarationController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = mockDelegationConnector
    override val propertyDetailsService = mockService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector

  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockService)
    reset(mockDelegationConnector)
    reset(mockBackLinkCache)
  }

  "PropertyDetailsDeclarationController" must {

    "use correct DelegationConnector" in {
      PropertyDetailsDeclarationController.delegationConnector must be(FrontendDelegationConnector)
    }

    "use correct service" in {
      PropertyDetailsDeclarationController.propertyDetailsService must be(PropertyDetailsService)
    }

    "view" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/liability/create/declaration/1"))
        result.isDefined must be(true)
        status(result.get) must not be(NOT_FOUND)
      }

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

        "have a status of ok, for clients" in {
          getWithAuthorisedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Returns declaration")
              document.getElementById("chargeable-return-before-declaration-text").text() must be("Before you can submit your return to HMRC you must read and agree to the following statement. If you give false information you may have to pay financial penalties and face prosecution.")
              document.getElementById("declaration-confirmation-text").text() must be("I declare that the information I have given on this return is correct and complete.")
              document.getElementById("submit").text() must be("Agree and submit return")
          }
        }
        "have a status of ok, for agents" in {
          getWithAuthorisedDelegatedUser {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("Returns declaration")
              document.getElementById("chargeable-return-before-declaration-text").text() must be("Before your client's return can be submitted to HMRC, you must read and agree to the following statement. Your client's approval may be in electronic or non-electronic form. If your client gives false information, they may have to pay financial penalties and face prosecution.")
              document.getElementById("declaration-confirmation-text").text() must be("I confirm that my client has approved the information contained in this return as being correct and complete to the best of their knowledge and belief.")
              document.getElementById("submit").text() must be("Agree and submit return")
          }
        }
      }
    }

    "submit" must {

      "unauthorised users" must {

        "be redirected to the login page" in {
          submitWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "for valid data, return OK" in {
          val inputJson = Json.parse("""{}""")
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson) {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }
        "for valid data, return BAD_REQUEST" in {
          val inputJson = Json.parse("""{}""")
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedInvalidAgent(inputJson) {
            result =>
              status(result) must be(BAD_REQUEST)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be("There was a problem when you set up this client")
          }
        }
      }

    }
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsDeclarationController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestPropertyDetailsDeclarationController.view("1").apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
      Future.successful(PropertyDetailsCacheSuccessResponse(PropertyDetailsBuilder.getPropertyDetails("1")))
    }
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPropertyDetailsDeclarationController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedDelegatedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createDelegatedAuthContext(userId, "company name|display name"))
    implicit val hc: HeaderCarrier = HeaderCarrier(userId = Some(UserId(userId)))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDelegationConnector.getDelegationData(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(DummyDelegationData.returnData)))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
      Future.successful(PropertyDetailsCacheSuccessResponse(PropertyDetailsBuilder.getPropertyDetails("1")))
    }
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPropertyDetailsDeclarationController.view("1").apply(SessionBuilder.buildRequestWithSessionDelegation(userId))
    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsDeclarationController.submit("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestPropertyDetailsDeclarationController.submit("1").apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUser(inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    when(mockService.submitDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.toJson(propertyDetails)))))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsDeclarationController.submit("1").apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))

    test(result)
  }

  def submitWithAuthorisedInvalidAgent(inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    when(mockService.submitDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(Json.parse("""{"Reason":"Agent not Valid"}""")))))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsDeclarationController.submit("1").apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))

    test(result)
  }
  def submitWithAuthorisedOtherUsers(inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
    when(mockService.retrieveDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    when(mockService.submitDraftPropertyDetails(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR,None)))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestPropertyDetailsDeclarationController.submit("1").apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(), userId))

    test(result)
  }
}
