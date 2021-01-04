/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.SelectPeriod
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ServiceInfoService
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, PeriodUtils}
import views.html.BtaNavigationLinks

import scala.concurrent.Future

class SelectPeriodControllerSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockReturnTypeController: ReturnTypeController = mock[ReturnTypeController]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance = app.injector.instanceOf[views.html.selectPeriod]

  val baseYear = 2018

  class Setup(endYear : Int = baseYear) {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testSelectPeriodController: SelectPeriodController = new SelectPeriodController(
      mockMcc,
      mockAuthAction,
      mockServiceInfoService,
      mockBackLinkCacheConnector,
      mockDataCacheConnector,
      injectedViewInstance
    ) {
      override def endDate(): LocalDate = LocalDate.parse(s"$endYear-05-20")
    }

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

      val result = testSelectPeriodController.view.apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUserWithSavedData(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))

      when(mockDataCacheConnector.fetchAndGetFormData[SelectPeriod](ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(SelectPeriod(Some("2015")))))

      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testSelectPeriodController.view.apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val result = testSelectPeriodController.view.apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson], atedRef: Option[String] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(atedRef))
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testSelectPeriodController.submit.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
      test(result)
    }
  }

  "SelectPeriodController" should {
    "selectPeriod" must {
      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }
      }

      "be redirected to the unauthorised page" in new Setup {
        getWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }

      "Authorised users" must {

        "show the select period view" in new Setup {
          getWithAuthorisedUser { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select an ATED chargeable period"))
            document.getElementById("header").text() must include("Select an ATED chargeable period")
            document.getElementById("details-text").text() must be("The chargeable period for a year runs from the 1 April to 31 March.")
            document.getElementById("period-2015_field").text() must be("2015 to 2016")
            document.getElementById("period-2016_field").text() must be("2016 to 2017")
            document.getElementById("period-2017_field").text() must be("2017 to 2018")
            document.getElementById("submit").text() must be("Continue")
          }
        }

        "show the select period view for 2017" in new Setup(2017) {
          getWithAuthorisedUser { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select an ATED chargeable period"))
            document.getElementById("header").text() must include("Select an ATED chargeable period")
            document.getElementById("details-text").text() must be("The chargeable period for a year runs from the 1 April to 31 March.")
            document.getElementById("period-2015_field").text() must be("2015 to 2016")
            document.getElementById("period-2016_field").text() must be("2016 to 2017")
            document.getElementById("submit").text() must be("Continue")
          }
        }

        "show the select period view for 2020" in new Setup(2020) {
          getWithAuthorisedUser { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select an ATED chargeable period"))
            document.getElementById("header").text() must include("Select an ATED chargeable period")
            document.getElementById("details-text").text() must be("The chargeable period for a year runs from the 1 April to 31 March.")
            document.getElementById("period-2015_field").text() must be("2015 to 2016")
            document.getElementById("period-2016_field").text() must be("2016 to 2017")
            document.getElementById("period-2019_field").text() must be("2019 to 2020")
            document.getElementById("submit").text() must be("Continue")
          }
        }

        "show the select period view with data if we have some" in new Setup {
          getWithAuthorisedUserWithSavedData { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select an ATED chargeable period"))
            document.getElementById("header").text() must include("Select an ATED chargeable period")
            document.getElementById("details-text").text() must be("The chargeable period for a year runs from the 1 April to 31 March.")
            document.getElementById("period-2015_field").text() must be("2015 to 2016")
            document.getElementById("period-2015").attr("checked") must be("checked")
            document.getElementById("period-2016_field").text() must be("2016 to 2017")
            document.getElementById("period-2017_field").text() must be("2017 to 2018")
            document.getElementById("submit").text() must be("Continue")
          }
        }
      }

      "submit" must {
        "for authorised user" must {

          "with invalid form, return BadRequest" in new Setup {
            val peakStartYear: Int = PeriodUtils.calculatePeakStartYear()
            val inputJson: JsValue = Json.parse( """{"returnType": ""}""")
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson), Some("XN1200000100001")) {
              result =>
                status(result) must be(BAD_REQUEST)
                val doc = Jsoup.parse(contentAsString(result))

                doc.getElementsByClass("error-notification").html() must include("Select an option for type of return")
                contentAsString(result) must include("Select an option for type of return")
                doc.getElementById("period-2015_field").text() must be("2015 to 2016")
                doc.getElementById("period-2016_field").text() must be("2016 to 2017")
                doc.getElementById("period-2017_field").text() must be("2017 to 2018")
                assert(doc.getElementById(s"period-${peakStartYear}_field") === null)
            }
          }

          "with period=2015 Redirect to select return type page" in new Setup {
            val inputJson: JsValue = Json.parse( """{"period": "2015"}""")
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson), Some("XN1200000100001")) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must be("/ated/return-type/2015")
            }
          }

          "with period=2016 - Redirect to select return type page" in new Setup {
            val inputJson: JsValue = Json.parse( """{"period": "2016"}""")
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson), Some("XN1200000100001")) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must be("/ated/return-type/2016")
            }
          }

          "with period=2017 - Redirect to select return type page" in new Setup {
            val inputJson: JsValue = Json.parse( """{"period": "2017"}""")
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson), Some("XN1200000100001")) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must be("/ated/return-type/2017")
            }
          }

          "redirect to error page, if clients do not match" in new Setup {
            val inputJson: JsValue = Json.parse( """{"period": "2016"}""")
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
              result =>
                val document = Jsoup.parse(contentAsString(result))
                status(result) must be(OK)
                document.getElementById("content").text() must include
                "There are one or more people from your organisation signed in with the same Government Gateway details"
            }
          }
        }
      }
    }
  }
}
