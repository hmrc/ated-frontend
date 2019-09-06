/*
 * Copyright 2019 HM Revenue & Customs
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
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{DelegationService, FormBundleReturnsService, SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.MockAuthUtil

import scala.concurrent.Future

class FormBundleReturnControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  val periodKey: Int = 2015

  val mockFormBundleReturnsService: FormBundleReturnsService = mock[FormBundleReturnsService]
  val mockSummaryReturnsService: SummaryReturnsService = mock[SummaryReturnsService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val formBundleNo1: String = "123456789012"
  val organisationName: String = "ACME Limited"
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  object TestFormBundleReturnController extends FormBundleReturnController {
    override val delegationService: DelegationService = mockDelegationService
    override val formBundleReturnsService: FormBundleReturnsService = mockFormBundleReturnsService
    override val summaryReturnsService: SummaryReturnsService = mockSummaryReturnsService
    override val subscriptionDataService: SubscriptionDataService = mockSubscriptionDataService
    override val authConnector: PlayAuthConnector = mockAuthConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockFormBundleReturnsService)
    reset(mockSummaryReturnsService)
    reset(mockSubscriptionDataService)
  }

  "FormBundleReturnController" must {

    "use correct DelegationConnector" in {
      FormBundleReturnController.delegationService must be(DelegationService)
    }

    "form bundle returnType" must {

      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the unauthorised page" in {
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

      val formBundleProp = FormBundleProperty(BigDecimal(bd1), new LocalDate("2015-09-08"), new LocalDate("2015-10-12"), "Relief",Some("Property developers"))
      val formBundleProp2 = FormBundleProperty(BigDecimal(bd2), new LocalDate("2015-10-12"), new LocalDate("2015-12-12"), "Relief", Some("Property developers"))
      val formBundleAddress = FormBundleAddress("1 addressLine1", "addressLine2", Some("addressLine3"), Some("AddressLine4"), Some("XX11XX"), "GB")
      val formBundlePropertyDetails = FormBundlePropertyDetails(Some("title here"), formBundleAddress, Some("additional details"))
      val viewReturn = FormBundleReturn("2014", formBundlePropertyDetails, Some(new LocalDate("2013-10-10")), Some(BigDecimal(bd1)),
        Some("ABCdefgh"), Some("PromABCdefgh"), Some("1234"), professionalValuation = true, ninetyDayRuleApplies = true, new LocalDate("2015-05-10"),
        BigDecimal(bd3), "1234567891", List(formBundleProp))

      val viewReturnMultipleValues = FormBundleReturn("2014", formBundlePropertyDetails, Some(new LocalDate("2013-10-10")), Some(BigDecimal(bd1)),
        Some("ABCdefgh"), Some("PromABCdefgh"), Some("1234"), professionalValuation = true, ninetyDayRuleApplies = true, new LocalDate("2015-05-10"),
        BigDecimal(bd3), "1234567891", List(formBundleProp, formBundleProp2))

      "show the return view with data when change is allowed" in {

        val changeablePeriod = SubmittedLiabilityReturns(formBundleNo = formBundleNo1,
          description = "",
          liabilityAmount = BigDecimal(432.12),
          dateFrom = new LocalDate("2015-09-08"),
          dateTo = new LocalDate("2015-09-08"),
          dateOfSubmission = new LocalDate("2015-09-08"),
          changeAllowed = true,
          paymentReference = "")

//        val chargeableSubmittedReturns = new SubmittedReturns(periodKey, Nil, List(changeablePeriod))
        val periodSummaryChargeable = PeriodSummaryReturns(periodKey = periodKey,
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
      "try to show the return view even when we have no data" in {

        getWithAuthorisedUser(None, None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("View return")
        }
      }

      "show the return view with data when change is not allowed" in {

        getWithAuthorisedUser(Some(viewReturn), None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("View return")

            assert(document.getElementById("submit") === null)

        }
      }

      "show a return with multiple values" in {
        val changeablePeriod = SubmittedLiabilityReturns(formBundleNo = formBundleNo1,
          description = "",
          liabilityAmount = BigDecimal(432.12),
          dateFrom = new LocalDate("2015-09-08"),
          dateTo = new LocalDate("2015-09-08"),
          dateOfSubmission = new LocalDate("2015-09-08"),
          changeAllowed = true,
          paymentReference = "")

//        val chargeableSubmittedReturns = new SubmittedReturns(periodKey, Nil, List(changeablePeriod))
        val periodSummaryChargeable = PeriodSummaryReturns(periodKey = periodKey,
          draftReturns = Nil,
          submittedReturns = Some(new SubmittedReturns(periodKey, Nil, List(changeablePeriod))))

        getWithAuthorisedUser(Some(viewReturnMultipleValues), Some(periodSummaryChargeable)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must include("View return")

            assert(document.getElementById("submit").text() === "Change return")

        }
      }
    }
  }

  def getWithAuthorisedUser(formBundleReturn: Option[FormBundleReturn], periodSummaries: Option[PeriodSummaryReturns] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)

    when(mockFormBundleReturnsService.getFormBundleReturns(Matchers.eq(formBundleNo1))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(formBundleReturn))

    when(mockSummaryReturnsService.getPeriodSummaryReturns(Matchers.eq(periodKey))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(periodSummaries))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = TestFormBundleReturnController.view(formBundleNo1, periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val periodKey: Int = 2014
    val userId = s"user-${UUID.randomUUID}"
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestFormBundleReturnController.view("12345678901", periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
