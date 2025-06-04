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

package controllers

import java.util.UUID
import builders.{SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheService, DataCacheConnector}
import controllers.auth.AuthAction
import controllers.propertyDetails.{AddressLookupController, PropertyDetailsAddressController}
import controllers.reliefs.ChooseReliefsController
import models.{PreviousReturns, ReturnType}

import java.time.LocalDate
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
import services.{ServiceInfoService, SummaryReturnsService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import utils.AtedConstants.RetrieveReturnTypeFormId
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class ReturnTypeControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier                                        = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig                              = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents                                  = app.injector.instanceOf[MessagesControllerComponents]
  val mockBackLinkCacheConnector: BackLinkCacheService                       = mock[BackLinkCacheService]
  val mockDataCacheConnector: DataCacheConnector                             = mock[DataCacheConnector]
  val mockSummaryReturnsService: SummaryReturnsService                       = mock[SummaryReturnsService]
  val mockAddressLookupController: AddressLookupController                   = mock[AddressLookupController]
  val mockPropertyDetailsAddressController: PropertyDetailsAddressController = mock[PropertyDetailsAddressController]
  val mockChooseReliefsController: ChooseReliefsController                   = mock[ChooseReliefsController]
  val messagesApi: MessagesApi                                               = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl                                   = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks                             = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService                             = mock[ServiceInfoService]
  val injectedViewInstance                                                   = app.injector.instanceOf[views.html.returnType]

  val periodKey: Int = 2015

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testReturnTypeController: ReturnTypeController = new ReturnTypeController(
      mockMcc,
      mockAuthAction,
      mockSummaryReturnsService,
      mockServiceInfoService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def getWithAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId   = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(
        mockDataCacheConnector
          .fetchAndGetData[ReturnType](ArgumentMatchers.eq(RetrieveReturnTypeFormId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(ReturnType(Some("CR")))))
      when(
        mockDataCacheConnector
          .fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("XN1200000100001")))
      val result = testReturnTypeController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUserWithSomeData(test: Future[Result] => Any): Unit = {
      val userId   = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
      when(mockDataCacheConnector.fetchAndGetData[ReturnType](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(ReturnType(Some("RR")))))
      when(
        mockDataCacheConnector
          .fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testReturnTypeController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId   = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testReturnTypeController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(prevReturns: Seq[PreviousReturns], fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any): Unit = {
      val userId   = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
      when(
        mockDataCacheConnector
          .fetchAndGetData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockSummaryReturnsService.getPreviousSubmittedLiabilityDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(prevReturns))
      when(mockBackLinkCacheConnector.clearBackLinks(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Nil))
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))
      val result = testReturnTypeController.submit(periodKey).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
      test(result)
    }

  }

  override def beforeEach(): Unit = {}

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
            document.title() must be(TitleBuilder.buildTitle("Select a type of return"))
            document.getElementsByTag("h1").text() must include("Select a type of return")
            document.getElementsByAttributeValue("for", "returnType-2").text() must be(
              "For a property or properties in relief where no ATED charge is due")
          }
        }
        "show the return type view with saved data" in new Setup {
          getWithAuthorisedUserWithSomeData { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select a type of return"))
            document.getElementsByTag("h1").text() must include("Select a type of return")
            document.getElementsByAttributeValue("for", "returnType-2").text() must be(
              "For a property or properties in relief where no ATED charge is due")
            document.getElementById("returnType-2").attr("checked") must not be null
          }
        }

      }
    }

    "submit" must {
      "for authorised user" must {
        val prevReturns = Seq(PreviousReturns("1, addressLine1", "12345678", LocalDate.parse("2015-04-02"), true))
        "with valid form data" must {
          "with invalid form, return BadRequest" in new Setup {
            val inputJson: JsValue = Json.parse("""{"returnType": ""}""")
            when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
            submitWithAuthorisedUser(prevReturns, FakeRequest().withJsonBody(inputJson)) { result =>
              status(result) must be(BAD_REQUEST)
              val doc = Jsoup.parse(contentAsString(result))
              doc.getElementsByClass("govuk-error-summary__list").html() must include("Select an option for type of return")
              contentAsString(result) must include("Select an option for type of return")
            }
          }
          "with returnType=RR - relief return Redirect to choose relief page" in new Setup {
            val inputJson: JsValue = Json.parse("""{"returnType": "RR"}""")
            submitWithAuthorisedUser(prevReturns, FakeRequest().withJsonBody(inputJson)) { result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must be("/ated/reliefs/2015/choose")
            }
          }
          "with returnType=CR - chargeable return, status is OK" in new Setup {
            val inputJson: JsValue = Json.parse("""{"returnType": "CR"}""")
            submitWithAuthorisedUser(prevReturns, FakeRequest().withJsonBody(inputJson)) { result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must be("/ated/existing-return/confirmation/2015/charge")
            }
          }

          "with returnType=CR - chargeable return and previous submitted details found from ETMP, status is OK" in new Setup {
            val inputJson: JsValue = Json.parse("""{"returnType": "CR"}""")
            submitWithAuthorisedUser(Nil, FakeRequest().withJsonBody(inputJson)) { result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must be("/ated/liability/address-lookup/view/2015")
            }
          }
        }
        "with returnType=anything else, status is Redirect" in new Setup {
          val inputJson: JsValue = Json.parse("""{"returnType": "INVALID"}""")
          submitWithAuthorisedUser(prevReturns, FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must be("/ated/return-type/2015")
          }
        }
      }
    }
  }

}
