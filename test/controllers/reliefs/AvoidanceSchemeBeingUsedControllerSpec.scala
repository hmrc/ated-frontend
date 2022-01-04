/*
 * Copyright 2022 HM Revenue & Customs
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

import builders.{ReliefBuilder, SessionBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import controllers.propertyDetails.PropertyDetailsAddressController
import models.{Reliefs, ReliefsTaxAvoidance, TaxAvoidance}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContentAsFormUrlEncoded, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{FormBundleReturnsService, PropertyDetailsService, ReliefsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class AvoidanceSchemeBeingUsedControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockAvoidanceSchemesController: AvoidanceSchemesController = mock[AvoidanceSchemesController]
  val mockPropertyDetailsAddressController: PropertyDetailsAddressController = mock[PropertyDetailsAddressController]
  val mockFormBundleReturnsService: FormBundleReturnsService = mock[FormBundleReturnsService]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.reliefs.avoidanceSchemeBeingUsed]
  val injectedViewInstanceKey = app.injector.instanceOf[views.html.reliefs.invalidPeriodKey]

  val periodKey = 2015
  val testReliefs: ReliefsTaxAvoidance =
    ReliefBuilder.reliefTaxAvoidance(periodKey,Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(false)))
  val testReliefs1: ReliefsTaxAvoidance =
    ReliefBuilder.reliefTaxAvoidance(periodKey,Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)))
  val testReliefsWithTaxAvoidance: ReliefsTaxAvoidance =
    ReliefBuilder.reliefTaxAvoidance(periodKey,Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)))
  val testReliefsWithTaxAvoidancePopulated: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey,
    Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)),
    TaxAvoidance(rentalBusinessScheme = Some("avoid1"))
  )

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testAvoidanceSchemeBeingUsedController: AvoidanceSchemeBeingUsedController = new AvoidanceSchemeBeingUsedController(
      mockMcc,
      mockServiceInfoService,
      mockAuthAction,
      mockAvoidanceSchemesController,
      mockReliefsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance,
      injectedViewInstanceKey
    )

    def getAuthorisedUserNone(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockReliefsService.retrieveDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testAvoidanceSchemeBeingUsedController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def getAuthorisedUserSome(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))
      when(mockReliefsService.retrieveDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(testReliefs)))
      val result = testAvoidanceSchemeBeingUsedController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def getForbiddenUserSome(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setForbiddenAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockReliefsService.retrieveDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testAvoidanceSchemeBeingUsedController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testAvoidanceSchemeBeingUsedController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.saveDraftIsTaxAvoidance
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testReliefs)))
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testAvoidanceSchemeBeingUsedController.send(periodKey).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

      test(result)
    }

    def submitForbiddenUserNone(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.saveDraftIsTaxAvoidance
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testReliefs)))
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setForbiddenAuthMocks(authMock)
      val result = testAvoidanceSchemeBeingUsedController.send(periodKey).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

      test(result)
    }



    def editFromSummary(reliefs: Option[ReliefsTaxAvoidance]= None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.retrieveDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(reliefs))
      val result = testAvoidanceSchemeBeingUsedController.editFromSummary(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def editForbiddenUserNone(reliefs: Option[ReliefsTaxAvoidance]= None)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setForbiddenAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.retrieveDraftReliefs(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(reliefs))
      val result = testAvoidanceSchemeBeingUsedController.editFromSummary(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }
  }


  override def beforeEach(): Unit = {
  }

  "AvoidanceSchemeBeingUsedController" must {

    "view" must {

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

        "respond with a redirect to unauthorised URL" in new Setup {
          getForbiddenUserSome { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "return a status of OK && Avoidance scheme being used page be displayed empty v2.0" in new Setup {
          getAuthorisedUserSome {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.select(".block-label").text() must include("Yes")
              document.select(".block-label").text() must include("No")          }
        }

      }
      "contain the tax avoidance scheme radio buttons" in new Setup {
        getAuthorisedUserNone {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.select(".block-label").text() must include("Yes")
            document.select(".block-label").text() must include("No")
            document.getElementById("isAvoidanceScheme-true").attr("checked") must be("")
            document.getElementById("isAvoidanceScheme-false").attr("checked") must be("")
        }
      }

      "show an error if radio button is not selected" in new Setup {
        val formBody = List(
          ("periodKey", "2015"),
          ("isAvoidanceScheme", ""))
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(formBody: _*)) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("There is a problem with the avoidance scheme question")
        }
      }
    }

    "editFromSummary" must {

      "return a status of SEE_OTHER and have the back link set to the summary page when we have no data" in new Setup {
        editFromSummary(None) {
          result =>
            status(result) must be(SEE_OTHER)
        }
      }

      "return a status of SEE_OTHER and have the back link set to the summary page" in new Setup {
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        editFromSummary(Some(testReliefsWithTaxAvoidance)) {
          result =>
            status(result) must be(SEE_OTHER)
        }
      }

      "respond with a redirect to unauthorised URL" in new Setup {
        editForbiddenUserNone(None) { result =>
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }

    "submit" must {

      "Authorised users" must {

        "for invalid data, return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("periodKey", "2015"),
            ("isAvoidanceScheme", ""))
          when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(formBody: _*)) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for valid data, redirect to Avoidance scheme page" in new Setup {
          val formBody = List(
            ("periodKey", "2015"),
            ("isAvoidanceScheme", "true"))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(formBody: _*)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/ated/reliefs/2015/avoidance-schemes")

          }
        }

        "respond with a redirect to unauthorised URL" in new Setup {
          val formBody = List(
            ("periodKey", "2015"),
            ("isAvoidanceScheme", "true"))
          submitForbiddenUserNone(FakeRequest().withFormUrlEncodedBody(formBody: _*)) { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }
    }
  }
}
