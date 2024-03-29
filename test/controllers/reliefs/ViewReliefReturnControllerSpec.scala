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

package controllers.reliefs

import java.util.UUID
import builders.SessionBuilder
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.SubmittedReliefReturns
import java.time.LocalDate
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
import services.{ReliefsService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.reliefs.viewReliefReturn

import scala.concurrent.Future

class ViewReliefReturnControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockReliefDeclarationController: ReliefDeclarationController = mock[ReliefDeclarationController]
  val mockChangeReliefReturnController: ChangeReliefReturnController = mock[ChangeReliefReturnController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: viewReliefReturn = app.injector.instanceOf[views.html.reliefs.viewReliefReturn]

  val periodKey = 2015
  val formBundleNo = "1234567890"
  val organisationName = "ACME Limited"

  override def beforeEach(): Unit = {

  }

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testViewReliefReturnController: ViewReliefReturnController = new ViewReliefReturnController(
      mockMcc,
      mockAuthAction,
      mockSubscriptionDataService,
      mockChangeReliefReturnController,
      mockServiceInfoService,
      mockReliefsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def getWithAuthorisedUserSuccess(test: Future[Result] => Any, isEditable: Boolean = true): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val submittedReturns = SubmittedReliefReturns(
        formBundleNo, "Property rental businesses", LocalDate.parse("2015-05-01"), LocalDate.parse("2015-05-01"), LocalDate.parse("2015-05-01"), None, None)
      when(mockReliefsService.viewReliefReturn(ArgumentMatchers.eq(periodKey), ArgumentMatchers.eq(formBundleNo))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Tuple2(Some(submittedReturns), isEditable)))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))
      val result = testViewReliefReturnController.viewReliefReturn(periodKey, formBundleNo).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUserFailure(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))
      when(mockReliefsService.viewReliefReturn(ArgumentMatchers.eq(periodKey), ArgumentMatchers.eq(formBundleNo))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Tuple2(None, false)))
      val result = testViewReliefReturnController.viewReliefReturn(periodKey, formBundleNo).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testViewReliefReturnController.viewReliefReturn(periodKey, formBundleNo).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(test: Future[Result] => Any): Unit = {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(organisationName)))
      when(mockBackLinkCacheConnector.clearBackLinks(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Nil))
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("http://backLink")))
      val result = testViewReliefReturnController.submit(periodKey, "").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  "ViewReliefReturnController" must {
    "viewReliefReturn" must {
      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in new Setup {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "respond with OK, if summary returns are in cache" in new Setup {
          getWithAuthorisedUserSuccess {
            result =>
              status(result) must be(OK)
              val doc = Jsoup.parse(contentAsString(result))
              doc.getElementsByClass("govuk-caption-xl").text() contains ("This section is: " + organisationName)
              doc.getElementById("relief-return-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              doc.getElementsByTag("h1").text() contains "View return"
              doc.getElementById("submit").text() must be("Change return")
              doc.title() must be("View return - Submit and view your ATED returns - GOV.UK")
              assert(doc.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")

              doc.getElementsByClass("govuk-back-link").text must be("Back")
              doc.getElementsByClass("govuk-back-link").attr("href") must include("/ated/period-summary/2015")
          }
        }

        "respond with OK, if summary returns are in cache, but not editable" in new Setup {
          getWithAuthorisedUserSuccess (
            result => {
              status(result) must be(OK)
              val doc = Jsoup.parse(contentAsString(result))
              doc.getElementsByClass("govuk-caption-xl").text() contains ("This section is: " + organisationName)
              doc.getElementById("relief-return-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              doc.getElementsByTag("h1").text() contains "View return"
              assert(doc.getElementById("submit") === null)
              doc.title() must be("View return - Submit and view your ATED returns - GOV.UK")
            }, isEditable = false
          )
        }

        "be redirected to change relief page" in new Setup {
          submitWithAuthorisedUser {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must be("/ated/reliefs/2015/change?formBundleNo=")

          }
        }

          "respond with Exception, if summary returns are NOT in cache" in new Setup {
            getWithAuthorisedUserFailure {
              result =>
                val thrown = the[RuntimeException] thrownBy await(result)
                thrown.getMessage must be("No reliefs found in the cache for provided period and form bundle id")
            }
          }

      }
    }
  }
}
