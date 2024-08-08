/*
 * Copyright 2023 HM Revenue & Customs
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
import builders.{PropertyDetailsBuilder, SessionBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.propertyDetails.propertyDetailsRevalued

import scala.concurrent.Future

class PropertyDetailsRevaluedControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockIsFullTaxPeriodController: IsFullTaxPeriodController = mock[IsFullTaxPeriodController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: propertyDetailsRevalued = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsRevalued]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testPropertyDetailsRevaluedController: PropertyDetailsRevaluedController = new PropertyDetailsRevaluedController(
      mockMcc,
      mockAuthAction,
      mockIsFullTaxPeriodController,
      mockServiceInfoService,
      mockPropertyDetailsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsRevaluedController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testPropertyDetailsRevaluedController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getDataWithAuthorisedUser(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
      val result = testPropertyDetailsRevaluedController.view(id).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testPropertyDetailsRevaluedController.save("1", periodKey, None).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(inputJson: JsValue)(test: Future[Result] => Any): Unit = {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockPropertyDetailsService.saveDraftPropertyDetailsRevalued(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
        thenReturn(Future.successful(OK))
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testPropertyDetailsRevaluedController.save("1", periodKey, None)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
  }

  "PropertyDetailsRevaluedController" must {
    "propertyDetails" must {

      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in new Setup {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the chargeable property details view if we have id and data" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode")).copy(value = None)
          getDataWithAuthorisedUser("1", propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              assert(document.getElementsByTag("h1").text contains "Has the property been revalued since the £40,000 or more change?")
              assert(document.getElementsByClass("govuk-caption-xl").text === "This section is: Create return")
              assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
              document.getElementsByAttributeValue("for", "isPropertyRevalued").text() must be ("Yes")
              document.getElementsByAttributeValue("for", "isPropertyRevalued-2").text() must be ("No")
              document.getElementById("revaluedValue").attr("type") must be("text")
              document.getElementById("revaluedValue").attr("value") must be("")
              document.select("#conditional-isPropertyRevalued > div:nth-child(1) > fieldset > legend").text() must be ("What date did you make the £40,000 change?")
              document.getElementsByAttributeValue("for","revaluedValue").text() must be("What is the new valuation of the property in GBP?")
              document.select("#conditional-isPropertyRevalued > div:nth-child(3) > fieldset > legend").text() must be ("What date did you get the property revalued?")
              document.getElementById("isPropertyRevalued").attr("value") must be("true")
              document.getElementById("isPropertyRevalued-2").attr("value") must be("false")
              document.getElementById("isPropertyRevalued").hasAttr("checked") must be(false)
              document.getElementById("isPropertyRevalued-2").hasAttr("checked") must be(false)
              document.getElementById("partAcqDispDate.day").attr("value") must be("")
              document.getElementById("partAcqDispDate.month").attr("value") must be("")
              document.getElementById("partAcqDispDate.year").attr("value") must be("")
              document.getElementById("revaluedDate.day").attr("value") must be("")
              document.getElementById("revaluedDate.month").attr("value") must be("")
              document.getElementById("revaluedDate.year").attr("value") must be("")
              document.getElementById("submit").text() must be ("Save and continue")
          }
        }

        "show the chargeable property details view if we have id and data even with a propertyDetails value" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          getDataWithAuthorisedUser("1", propertyDetails) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              assert(document.getElementsByTag("h1").text contains "Has the property been revalued since the £40,000 or more change?")
              assert(document.getElementsByClass("govuk-caption-xl").text === "This section is: Create return")
              document.getElementsByAttributeValue("for", "isPropertyRevalued").text() must be ("Yes")
              document.getElementsByAttributeValue("for", "isPropertyRevalued-2").text() must be ("No")
              document.select("#conditional-isPropertyRevalued > div:nth-child(1) > fieldset > legend").text() must be ("What date did you make the £40,000 change?")
              document.getElementsByAttributeValue("for","revaluedValue").text() must be("What is the new valuation of the property in GBP?")
              document.select("#conditional-isPropertyRevalued > div:nth-child(3) > fieldset > legend").text() must be ("What date did you get the property revalued?")
              document.getElementById("revaluedValue").attr("type") must be("text")
              document.getElementsByClass("govuk-fieldset__legend").text.contains("What date did you get the property revalued?")
              document.getElementById("isPropertyRevalued").attr("value") must be("true")
              document.getElementById("isPropertyRevalued-2").attr("value") must be("false")
              document.getElementById("isPropertyRevalued").hasAttr("checked") must be(true)
              document.getElementById("isPropertyRevalued-2").hasAttr("checked") must be(false)
              document.getElementById("revaluedDate.day").attr("value") must be("1")
              document.getElementById("revaluedDate.month").attr("value") must be("4")
              document.getElementById("revaluedDate.year").attr("value") must be("2019")
              document.getElementById("submit").text() must be ("Save and continue")
          }
        }
      }
    }

    "save" must {
      "unauthorised users" must {

        "be redirected to the login page" in new Setup {
          saveWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "for invalid data, return BAD_REQUEST" in new Setup {

          val inputJson: JsValue = Json.toJson(
            PropertyDetailsRevalued(Some(true))
          )
          when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(inputJson) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "When the data is valid forward to the Period Page" in new Setup {
          when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(Json.toJson(
            PropertyDetailsRevalued(Some(false)))) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/full-tax-period/view")
          }
        }
      }
    }
  }
}
