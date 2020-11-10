/*
 * Copyright 2020 HM Revenue & Customs
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

/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import java.util.UUID

import builders.{SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.{AgentClientMandateFrontendConnector, BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.ReturnType
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class ExistingReturnQuestionControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val template = app.injector.instanceOf[views.html.confirmPastReturn]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockSummaryReturnsService: SummaryReturnsService = mock[SummaryReturnsService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockMandateFrontendConnector: AgentClientMandateFrontendConnector = mock[AgentClientMandateFrontendConnector]
  val mockDetailsService: DetailsService = mock[DetailsService]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val confirmAddressUrl: String = "http://localhost:9916/ated/liability/confirm-address/view/2017/123456789026?mode=editSubmitted"


  val periodKey: Int = 2015
  val returnTypeCharge: String = "CR"
  val returnTypeRelief: String = "RR"

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testExistingReturnQuestionController: ExistingReturnQuestionController = new ExistingReturnQuestionController (
      mockMcc,
      mockAuthAction,
      mockServiceInfoService,
      mockDataCacheConnector,
      template
    )

    def getWithAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDataCacheConnector.fetchAndGetFormData[String](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testExistingReturnQuestionController.view(periodKey, returnTypeCharge).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUserWithSomeData(test: Future[Result] => Any, fromConfirmAddress: Boolean = false) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDataCacheConnector.fetchAndGetFormData[ReturnType](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(ReturnType(Some("RR")))))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

      val fakeRequest = if(fromConfirmAddress) {
        SessionBuilder.buildRequestWithSession(userId).withHeaders("referer" -> confirmAddressUrl)
      }else{
        SessionBuilder.buildRequestWithSession(userId)
      }

      val result = testExistingReturnQuestionController.view(periodKey, returnTypeRelief)
        .apply(fakeRequest)
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testExistingReturnQuestionController.view(periodKey, returnTypeCharge).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthenticated(test: Future[Result] => Any) {
      val result = testExistingReturnQuestionController.view(periodKey, returnTypeCharge).apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.clearBackLinks(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Nil))
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testExistingReturnQuestionController.submit(periodKey, returnTypeCharge)
        .apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
  }

  "ReturnTypeController" must {
    "returnType" must {

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
        "show the return type view in version 2" in new Setup {
          getWithAuthorisedUser { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Did you file a return for this property in 2014 to 2015?"))
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
            document.getElementById("return-type-header")
              .text() must be("Did you file a return for this property in 2014 to 2015?")
          }
        }
        "show the return type view with saved data" in new Setup {
          getWithAuthorisedUserWithSomeData { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Did you file a return for this property in 2014 to 2015?"))
            document.getElementById("return-type-header")
              .text() must be("Did you file a return for this property in 2014 to 2015?")
          }
        }
        "have a back link which goes to the address confirmation" when {
          "the user came from the address confirmation when editing an existing return" in new Setup {
            getWithAuthorisedUserWithSomeData(result => {
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.getElementById("backLinkHref").attr("href") must be("/ated/liability/confirm-address/view/2017/123456789026?mode=editSubmitted")
            }, fromConfirmAddress = true)
          }
        }
      }
    }

    "submit" must {
      "for authorised user" must {
        "with valid form data" must {
          "with invalid form, return BadRequest" in new Setup {
            val inputJson: JsValue = Json.parse( """{"yesNo": ""}""")
            when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                val doc = Jsoup.parse(contentAsString(result))
                doc.getElementsByClass("error-notification").html() must include("Select yes if you filed a return for this property last year")
            }
          }

          "with returnType=CR - chargeable return, status is OK" in new Setup {
            val inputJson: JsValue = Json.parse( """{"yesNo": "true"}""")
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must be("/ated/existing-return/select/2015/CR")
            }
          }

          "with returnType=anything else, status is Redirect" in new Setup {
            val inputJson: JsValue = Json.parse( """{"yesNo": "false"}""")
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must be("/ated/liability/address-lookup/view/2015")
            }
          }
        }
      }
    }
  }
}
