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

import builders.SessionBuilder
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.AuthAction
import models._
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
import services.{FormBundleReturnsService, ServiceInfoService, SubscriptionDataService, SummaryReturnsService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import views.html.{BtaNavigationLinks, formBundleReturn}

import scala.concurrent.Future

class FormBundleReturnControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockFormBundleReturnsService: FormBundleReturnsService = mock[FormBundleReturnsService]
  val mockSummaryReturnsService: SummaryReturnsService = mock[SummaryReturnsService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: formBundleReturn = app.injector.instanceOf[views.html.formBundleReturn]

  val periodKey: Int = 2015
  val formBundleNo1: String = "123456789012"
  val organisationName: String = "ACME Limited"

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testFormBundleReturnController: FormBundleReturnController = new FormBundleReturnController (
      mockMcc,
      mockAuthAction,
      mockFormBundleReturnsService,
      mockSummaryReturnsService,
      mockServiceInfoService,
      mockSubscriptionDataService,
      injectedViewInstance
    )

    def getWithAuthorisedUser(formBundleReturn: Option[FormBundleReturn], periodSummaries: Option[PeriodSummaryReturns] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)

      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))

      when(mockFormBundleReturnsService.getFormBundleReturns(ArgumentMatchers.eq(formBundleNo1))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(formBundleReturn))

      when(mockSummaryReturnsService.getPeriodSummaryReturns(ArgumentMatchers.eq(periodKey))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(periodSummaries))

      when(mockSubscriptionDataService.getOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(organisationName)))

      val result = testFormBundleReturnController.view(formBundleNo1, periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val periodKey: Int = 2014
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testFormBundleReturnController.view("12345678901", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
  }

  "FormBundleReturnController" must {
    "form bundle returnType" must {
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
    }

    "Authorised users" must {
      val bd1: Int = 100
      val bd2: Int = 200
      val bd3: Int = 9324

      val formBundleProp = FormBundleProperty(BigDecimal(bd1), LocalDate.parse("2015-09-08"), LocalDate.parse("2015-10-12"), "Relief",Some("Property developers"))
      val formBundleProp2 = FormBundleProperty(BigDecimal(bd2), LocalDate.parse("2015-10-12"), LocalDate.parse("2015-12-12"), "Relief", Some("Property developers"))
      val formBundleAddress = FormBundleAddress("1 addressLine1", "addressLine2", Some("addressLine3"), Some("AddressLine4"), Some("XX11XX"), "GB")
      val formBundlePropertyDetails = FormBundlePropertyDetails(Some("title here"), formBundleAddress, Some("additional details"))
      val viewReturn = FormBundleReturn("2014", formBundlePropertyDetails, Some(LocalDate.parse("2013-10-10")), Some(BigDecimal(bd1)),
        Some("ABCdefgh"), Some("PromABCdefgh"), Some("1234"), professionalValuation = true, ninetyDayRuleApplies = true, LocalDate.parse("2015-05-10"),
        BigDecimal(bd3), "1234567891", List(formBundleProp))

      val viewReturnMultipleValues = FormBundleReturn("2014", formBundlePropertyDetails, Some(LocalDate.parse("2013-10-10")), Some(BigDecimal(bd1)),
        Some("ABCdefgh"), Some("PromABCdefgh"), Some("1234"), professionalValuation = true, ninetyDayRuleApplies = true, LocalDate.parse("2015-05-10"),
        BigDecimal(bd3), "1234567891", List(formBundleProp, formBundleProp2))

      "show the return view with data when change is allowed" in new Setup {

        val changeablePeriod: SubmittedLiabilityReturns = SubmittedLiabilityReturns(formBundleNo = formBundleNo1,
          description = "",
          liabilityAmount = BigDecimal(432.12),
          dateFrom = LocalDate.parse("2015-09-08"),
          dateTo = LocalDate.parse("2015-09-08"),
          dateOfSubmission = LocalDate.parse("2015-09-08"),
          changeAllowed = true,
          paymentReference = "")

        val periodSummaryChargeable: PeriodSummaryReturns = PeriodSummaryReturns(periodKey = periodKey,
          draftReturns = Nil,
          submittedReturns = Some(new SubmittedReturns(periodKey, Nil, List(changeablePeriod))))

        getWithAuthorisedUser(Some(viewReturn), Some(periodSummaryChargeable)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("View return")
            assert(document.getElementById("submit").text() === "Change return")

        }
      }
      "try to show the return view even when we have no data" in new Setup {

        getWithAuthorisedUser(None, None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("View return")
        }
      }

      "show the return view with data when change is not allowed" in new Setup {

        getWithAuthorisedUser(Some(viewReturn), None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("View return")

            assert(document.getElementById("submit") === null)
        }
      }

      "show a return with multiple values" in new Setup {
        val changeablePeriod: SubmittedLiabilityReturns = SubmittedLiabilityReturns(formBundleNo = formBundleNo1,
          description = "",
          liabilityAmount = BigDecimal(432.12),
          dateFrom = LocalDate.parse("2015-09-08"),
          dateTo = LocalDate.parse("2015-09-08"),
          dateOfSubmission = LocalDate.parse("2015-09-08"),
          changeAllowed = true,
          paymentReference = "")

        val periodSummaryChargeable: PeriodSummaryReturns = PeriodSummaryReturns(periodKey = periodKey,
          draftReturns = Nil,
          submittedReturns = Some(new SubmittedReturns(periodKey, Nil, List(changeablePeriod))))

        getWithAuthorisedUser(Some(viewReturnMultipleValues), Some(periodSummaryChargeable)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("View return")

            assert(document.getElementById("submit").text() === "Change return")
            document.getElementsByClass("govuk-back-link").text must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated/period-summary/" + periodKey)
        }
      }
    }
  }
}
