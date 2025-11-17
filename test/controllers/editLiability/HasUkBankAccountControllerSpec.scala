/*
 * Copyright 2025 HM Revenue & Customs
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

import builders._
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.{BankDetailsModel, PropertyDetails}
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
import services.{ChangeLiabilityReturnService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.editLiability.hasUkBankAccount

import java.util.UUID
import scala.concurrent.Future

class HasUkBankAccountControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockChangeLiabilityReturnService: ChangeLiabilityReturnService = mock[ChangeLiabilityReturnService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockEditLiabilitySummaryController: EditLiabilitySummaryController = mock[EditLiabilitySummaryController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: hasUkBankAccount = app.injector.instanceOf[views.html.editLiability.hasUkBankAccount]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testBankDetailsController: HasUkBankAccountController = new HasUkBankAccountController(
      mockMcc,
      mockChangeLiabilityReturnService,
      mockAuthAction,
      mockServiceInfoService,
      mockDataCacheConnector,
      mockBackLinkCache,
      injectedViewInstance
    )


    def viewWithAuthorisedUser(changeLiabilityReturnOpt: Option[PropertyDetails])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
      (ArgumentMatchers.eq("12345678901"), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(changeLiabilityReturnOpt))
      when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("http://backlink")))
      val result = testBankDetailsController.view("12345678901").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def editFromSummary(propertyDetails: Option[PropertyDetails])(test: Future[Result] => Any): Unit = {
      setAuthMocks(authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet))
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache
      (ArgumentMatchers.eq("12345678901"), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(propertyDetails))
      val result = testBankDetailsController.editFromSummary("12345678901")
        .apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithAuthorisedUser(inputJson: JsValue)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      val changeLiabilityReturn = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("123456789012")
      when(mockChangeLiabilityReturnService.cacheChangeLiabilityReturnBank
      (ArgumentMatchers.eq("12345678901"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(changeLiabilityReturn)))
      when(mockChangeLiabilityReturnService.cacheChangeLiabilityHasUkBankAccount
      (ArgumentMatchers.eq("12345678901"), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(changeLiabilityReturn)))
      val result = testBankDetailsController.save("12345678901").apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDelegationService)
    reset(mockChangeLiabilityReturnService)
    reset(mockBackLinkCache)
  }

  "BankController" must {

    "view - for authorised users" must {

      "navigate to bank details page, if liablity is retrieved" in new Setup {
        val bankDetails: BankDetailsModel = BankDetailsModel(hasBankDetails = false)
        val changeLiabilityReturn: PropertyDetails = ChangeLiabilityReturnBuilder
          .generateChangeLiabilityReturn("12345678901").copy(bankDetails = Some(bankDetails))
        viewWithAuthorisedUser(Some(changeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Are you using a UK bank account?"))
            document.getElementsByClass("govuk-caption-xl").text() must include("This section is: Change return")
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
            document.getElementsByClass("govuk-back-link").text must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must include("http://backlink")
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in new Setup {
        viewWithAuthorisedUser(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "edit - for authorised users" must {

      "return OK and set backlink to summary" in new Setup {
        val bankDetails: BankDetailsModel = BankDetailsModel(hasBankDetails = false)
        val returnData: PropertyDetails = ChangeLiabilityReturnBuilder
          .generateChangeLiabilityReturn("12345678901").copy(bankDetails = Some(bankDetails))

        editFromSummary(Some(returnData)) { result =>
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementsByClass("govuk-back-link").attr("href") must include("/summary")
        }
      }

      "redirect if no liability return found" in new Setup {
        editFromSummary(None) { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/ated/account-summary")
        }
      }
    }

    "save - for authorised user" must {
      "for invalid data, return BAD_REQUEST" in new Setup {
        val bankDetails: BankDetailsModel = BankDetailsModel(hasBankDetails = false)
        val inputJson: JsValue = Json.toJson(bankDetails)
        when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockChangeLiabilityReturnService, times(0))
              .cacheChangeLiabilityReturnBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "for a UK bank account selection, redirect to a page to enter UK bank details" in new Setup {
        val bankDetailsJson: JsValue = Json.parse(
          """{
            |"hasUkBankAccount": true
            |}""".stripMargin)
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(bankDetailsJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/uk-bank-details"))
            verify(mockChangeLiabilityReturnService, times(1))
              .cacheChangeLiabilityHasUkBankAccount(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "for a non-UK bank account selection, redirect to a page to enter non-UK bank details" in new Setup {
        val bankDetailsJson: JsValue = Json.parse(
          """{
            |"hasUkBankAccount": false
            |}""".stripMargin)
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(bankDetailsJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/12345678901/change/non-uk-bank-details"))
            verify(mockChangeLiabilityReturnService, times(1))
              .cacheChangeLiabilityHasUkBankAccount(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }

  }


}
