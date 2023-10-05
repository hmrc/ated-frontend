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

package controllers.editLiability

import java.util.UUID

import builders._
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.DisposeLiabilityReturn
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.{DisposeLiabilityReturnService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.editLiability.disposeLiabilitySummary

import scala.concurrent.Future

class DisposeLiabilitySummaryControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockDisposeLiabilityReturnService: DisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDisposeLiabilityDeclarationController: DisposeLiabilityDeclarationController = mock[DisposeLiabilityDeclarationController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: disposeLiabilitySummary = app.injector.instanceOf[views.html.editLiability.disposeLiabilitySummary]

  val organisationName: String = "ACME Limited"
  val oldFormBundleNum = "123456789012"

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction (
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testDisposeLiabilitySummaryController: DisposeLiabilitySummaryController = new DisposeLiabilitySummaryController(
      mockMcc,
      mockDisposeLiabilityReturnService,
      mockSubscriptionDataService,
      mockAuthAction,
      mockDisposeLiabilityDeclarationController,
      mockServiceInfoService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def viewWithAuthorisedUser(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("http://backlink")))
      when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(ArgumentMatchers.eq(oldFormBundleNum))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
      val result = testDisposeLiabilitySummaryController.view(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = testDisposeLiabilitySummaryController.submit(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def getPrintFriendlyWithAuthorisedUser(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(ArgumentMatchers.eq(oldFormBundleNum))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(organisationName)))
      val result = testDisposeLiabilitySummaryController.viewPrintFriendlyDisposeLiabilityReturn(oldFormBundleNum)
        .apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {

    reset(mockDisposeLiabilityReturnService)
    reset(mockSubscriptionDataService)
    reset(mockDelegationService)
    reset(mockDataCacheConnector)
    reset(mockBackLinkCacheConnector)
    reset(mockDisposeLiabilityDeclarationController)
  }

    "view" must {

      "return a status of OK, when that liability return is found in cache or ETMP" in new Setup {
        val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("12345678901")
        viewWithAuthorisedUser(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title must be("Check your details are correct - GOV.UK")
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
            document.getElementsByTag("h1").text() must include("Check your details are correct")
            document.getElementById("details-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
            document.getElementById("cya-bank-details").text() must include("Bank account to pay a refund INCOMPLETE Change")
            document.getElementById("cya-property-details-header").text() must be("Property details")
            document.select("#cya-property-details > dl > div:nth-child(2) > dd.govuk-summary-list__actions > a").text() must be("Change Date disposed property")
            document.getElementsByClass("govuk-summary-list__key").text() must include("Address")
            document.getElementById("address-line-1").text() must be("line1")
            document.getElementById("address-line-2").text() must be("line2")
            document.getElementsByClass("govuk-summary-list__key").text() must include("Date disposed property")
            document.getElementsByClass("govuk-summary-list__value").text() must include("2 April 2015")
            document.getElementById("print-friendly-edit-liability-link").text() must be("Print this return")
            document.getElementById("saved-returns-link").text() must be("Save as draft")
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

    "submit" must {
      "redirect to dispose-property declaration page" in new Setup {
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        submitWithAuthorisedUser {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/123456789012/dispose/declaration"))
        }
      }
    }

    "print friendly view" when {

      "called for authorised user" must {

        "return status OK" in new Setup {
          val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("12345678901")
          getPrintFriendlyWithAuthorisedUser(Some(disposeLiabilityReturn)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title must be("Check your details are correct")
              document.getElementById("edit-liability-summary-header").text() must be("Amended return for ACME Limited")
          }
        }

        "redirect to account summary page, when that liability return is not-found in cache or ETMP" in new Setup {
          getPrintFriendlyWithAuthorisedUser(None) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some("/ated/account-summary"))
          }
        }
      }
    }
  }
