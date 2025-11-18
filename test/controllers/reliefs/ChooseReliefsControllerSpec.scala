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

import builders.{ReliefBuilder, SessionBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheService, DataCacheService}
import controllers.auth.AuthAction
import models.{Reliefs, ReliefsTaxAvoidance, TaxAvoidance}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ReliefsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.reliefs.{chooseReliefs, invalidPeriodKey}

import java.util.UUID
import scala.concurrent.Future

class ChooseReliefsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockChooseReliefsController: ChooseReliefsController = mock[ChooseReliefsController]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockBackLinkCacheService: BackLinkCacheService = mock[BackLinkCacheService]
  val mockAvoidanceSchemeBeingUsedController: AvoidanceSchemeBeingUsedController = mock[AvoidanceSchemeBeingUsedController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: invalidPeriodKey = app.injector.instanceOf[views.html.reliefs.invalidPeriodKey]
  val injectedViewInstanceChooseChoose: chooseReliefs = app.injector.instanceOf[views.html.reliefs.chooseReliefs]

  val periodKey = 2015

  val reliefsTaxAvoid: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey,
    Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = None))
  val testReliefs: ReliefsTaxAvoidance =
    ReliefBuilder.reliefTaxAvoidance(periodKey, Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(false)))
  val testReliefsWithTaxAvoidance: ReliefsTaxAvoidance =
    ReliefBuilder.reliefTaxAvoidance(periodKey, Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)))
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

    val testChooseReliefsController: ChooseReliefsController = new ChooseReliefsController(
      mockMcc,
      mockAuthAction,
      mockAvoidanceSchemeBeingUsedController,
      mockServiceInfoService,
      mockReliefsService,
      mockDataCacheService,
      mockBackLinkCacheService,
      injectedViewInstance,
      injectedViewInstanceChooseChoose
    )

    def authorisedUserNone(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))
        (any(), any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheService.fetchAndGetBackLink(any())(any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheService.saveBackLink(any(), any())(any())).thenReturn(Future.successful(None))
      when(mockReliefsService.retrieveDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(None))
      val result = testChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def editFromSummary(reliefs: Option[ReliefsTaxAvoidance] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheService.fetchAndGetData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))
        (any(), any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.retrieveDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(reliefs))
      val result = testChooseReliefsController.editFromSummary(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def forbiddenEditFromSummary(reliefs: Option[ReliefsTaxAvoidance] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setForbiddenAuthMocks(authMock)
      when(mockDataCacheService.fetchAndGetData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))
        (any(), any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.retrieveDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(reliefs))
      val result = testChooseReliefsController.editFromSummary(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def authorisedUserSome(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheService.fetchAndGetData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))
        (any(), any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheService.fetchAndGetBackLink(any())(any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheService.saveBackLink(any(), any())(any())).thenReturn(Future.successful(None))
      when(mockReliefsService.retrieveDraftReliefs(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(testReliefs)))
      val result = testChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

      test(result)
    }

    def withUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], inputJson: JsValue)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheService.fetchAndGetData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))
        (any(), any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.saveDraftReliefs(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(testReliefs)))
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))

      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testChooseReliefsController.send(periodKey).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

      test(result)
    }

    def submitFormBodyWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheService.fetchAndGetData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))
        (any(), any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.saveDraftReliefs(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(testReliefs)))

      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      val result = testChooseReliefsController.send(periodKey).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

      test(result)
    }

    def forbiddenSubmitUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      when(mockDataCacheService.fetchAndGetData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))
        (any(), any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockReliefsService.saveDraftReliefs(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(testReliefs)))

      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setForbiddenAuthMocks(authMock)
      val result = testChooseReliefsController.send(periodKey).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

      test(result)
    }
  }

  override def beforeEach(): Unit = {
  }

  "ChooseReliefsController" must {
    "view" must {

      "unauthorised users" must {
        "respond with a redirect" in new Setup {
          withUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in new Setup {
          withUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "return a status of OK && Choose reliefs page be displayed empty v2.0" in new Setup {
          authorisedUserNone {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("lede-text")
                .text() must be("You can select more than one relief code. A single relief code can cover one or more properties.")

          }
        }

        "return a status of redirect && Choose reliefs page be displayed filled form, if something has been saved earlier" in new Setup {
          authorisedUserSome {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              assert(document.getElementById("rentalBusiness").outerHtml() contains "checked")
              assertResult(false)(document.getElementById("openToPublic").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("propertyDeveloper").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("propertyTrading").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("lending").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("employeeOccupation").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("farmHouses").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("socialHousing").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("equityRelease").outerHtml().contains("checked"))
          }
        }


        "return a status of OK && Choose reliefs page be displayed filled form, if nothing has been saved earlier" in new Setup {
          authorisedUserNone {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))


              assertResult(false)(document.getElementById("rentalBusiness").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("openToPublic").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("propertyDeveloper").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("propertyTrading").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("lending").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("employeeOccupation").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("farmHouses").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("socialHousing").outerHtml().contains("checked"))
              assertResult(false)(document.getElementById("equityRelease").outerHtml().contains("checked"))

          }
        }
      }

    }

    "editFromSummary" must {

      "return a status of OK and have the back link set to the summary page when we have no data" in new Setup {
        editFromSummary(None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("lede-text")
              .text() must be("You can select more than one relief code. A single relief code can cover one or more properties.")

            document.getElementsByClass("govuk-back-link").text must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/reliefs/2015/relief-summary")
        }
      }

      "return a status of OK and have the back link set to the summary page" in new Setup {
        editFromSummary(Some(testReliefs)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("lede-text")
              .text() must be("You can select more than one relief code. A single relief code can cover one or more properties.")

            document.getElementsByClass("govuk-back-link").text must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/reliefs/2015/relief-summary")
        }
      }

      "respond with a redirect to unauthorised URL" in new Setup {
        forbiddenEditFromSummary(Some(testReliefs)) { result =>
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }

    "submit" must {

      "Authorised users" must {

        "respond with a redirect to unauthorised URL for forbidden user" in new Setup {
          val formBody = List(
            ("periodKey", "2015"),
            ("rentalBusiness", "true"),
            ("isAvoidanceScheme", "true"),
            ("rentalBusinessDate.year", "2015"),
            ("rentalBusinessDate.month", "05"),
            ("rentalBusinessDate.day", "01"))
          forbiddenSubmitUser(FakeRequest().withFormUrlEncodedBody(formBody: _*)) { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }

        "for invalid data, return BAD_REQUEST" in new Setup {
          val inputJson: JsValue = Json.parse("""{"periodKey": 2015, "rentalBusiness": false, "isAvoidanceScheme": ""}""")
          when(mockBackLinkCacheService.fetchAndGetBackLink(any())(any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), inputJson) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for invalid data, return BAD_REQUEST v2.0" in new Setup {
          val inputJson: JsValue = Json.parse("""{"periodKey": 2015, "rentalBusiness": false, "isAvoidanceScheme": ""}""")
          when(mockBackLinkCacheService.fetchAndGetBackLink(any())(any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), inputJson) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for respective relief selected, respective dates become mandatory, so give BAD_REQUEST" in new Setup {
          val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, openToPublic = true, propertyDeveloper = true)
          val json: JsValue = Json.toJson(reliefs)
          when(mockBackLinkCacheService.fetchAndGetBackLink(any())(any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), json) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("There is a problem")
          }
        }

        "for all/any dates too early than period, return BAD_REQUEST" in new Setup {
          val inputJsonOne: JsValue = Json.parse(
            """{"periodKey": 2015,
              |"rentalBusiness": true,
              |"rentalBusinessDate.year": "2014",
              |"rentalBusinessDate.month": "05",
              |"rentalBusinessDate.day": "01",
              |"isAvoidanceScheme": true }""".stripMargin)
          when(mockBackLinkCacheService.fetchAndGetBackLink(any())(any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), inputJsonOne) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("There is a problem")
          }
        }
        "for all/any dates too late than period, return BAD_REQUEST" in new Setup {
          val inputJsonOne: JsValue = Json.parse(
            """{"periodKey": 2015,
              |"rentalBusiness": true,
              |"rentalBusinessDate.year": "2016",
              |"rentalBusinessDate.month": "05",
              |"rentalBusinessDate.day": "01",
              |"isAvoidanceScheme": true }""".stripMargin)
          when(mockBackLinkCacheService.fetchAndGetBackLink(any())(any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), inputJsonOne) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("There is a problem")
          }
        }
        "for all/any dates which are invalid, return BAD_REQUEST" in new Setup {
          val inputJsonOne: JsValue = Json.parse(
            """{"periodKey": 2015,
              |"rentalBusiness": true,
              |"rentalBusinessDate.year": "2016",
              |"rentalBusinessDate.month": "02",
              |"rentalBusinessDate.day": "31",
              |"isAvoidanceScheme": true }""".stripMargin)
          when(mockBackLinkCacheService.fetchAndGetBackLink(any())(any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), inputJsonOne) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("There is a problem")
          }
        }
        "for one date that is invalid but unselected, with valid data, return OK" in new Setup {
          val formBody = List(
            ("periodKey", "2015"),
            ("openToPublicDate.year", "2015"),
            ("openToPublicDate.month", "02"),
            ("openToPublicDate.year", "31"),
            ("rentalBusiness", "true"),
            ("isAvoidanceScheme", "true"),
            ("rentalBusinessDate.year", "2015"),
            ("rentalBusinessDate.month", "05"),
            ("rentalBusinessDate.day", "01"))
          when(mockBackLinkCacheService.saveBackLink(any(), any())(any())).thenReturn(Future.successful(None))
          submitFormBodyWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(formBody: _*)) {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }
        "for valid data, return OK" in new Setup {
          val formBody = List(
            ("periodKey", "2015"),
            ("rentalBusiness", "true"),
            ("isAvoidanceScheme", "true"),
            ("rentalBusinessDate.year", "2015"),
            ("rentalBusinessDate.month", "05"),
            ("rentalBusinessDate.day", "01"))
          when(mockBackLinkCacheService.saveBackLink(any(), any())(any())).thenReturn(Future.successful(None))
          submitFormBodyWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(formBody: _*)) {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }
      }
    }
  }
}
