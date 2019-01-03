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

import builders.{AuthBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{SubscriptionDataService, FormBundleReturnsService, SummaryReturnsService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}

import scala.concurrent.Future

class FormBundleReturnControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockFormBundleReturnsService = mock[FormBundleReturnsService]
  val mockSummaryReturnsService = mock[SummaryReturnsService]
  val mockSubscriptionDataService = mock[SubscriptionDataService]
  val formBundleNo1 = "123456789012"
  val organisationName = "ACME Limited"

  object TestFormBundleReturnController extends FormBundleReturnController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val formBundleReturnsService = mockFormBundleReturnsService
    override val summaryReturnsService = mockSummaryReturnsService
    override val subscriptionDataService = mockSubscriptionDataService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockFormBundleReturnsService)
    reset(mockSummaryReturnsService)
    reset(mockSubscriptionDataService)
  }

  "FormBundleReturnController" must {

    "use correct DelegationConnector" in {
      FormBundleReturnController.delegationConnector must be(FrontendDelegationConnector)
    }

    "form bundle returnType" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/form-bundle/12345678901090/2014"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
      }

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

      val formBundleProp = FormBundleProperty(BigDecimal(100), new LocalDate("2015-09-08"), new LocalDate("2015-10-12"), "Relief", Some("Property developers"))
      val formBundleProp2 = FormBundleProperty(BigDecimal(200), new LocalDate("2015-10-12"), new LocalDate("2015-12-12"), "Relief", Some("Property developers"))
      val formBundleAddress = FormBundleAddress("1 addressLine1", "addressLine2", Some("addressLine3"), Some("AddressLine4"), Some("XX11XX"), "GB")
      val formBundlePropertyDetails = FormBundlePropertyDetails(Some("title here"), formBundleAddress, Some("additional details"))
      val viewReturn = FormBundleReturn("2014", formBundlePropertyDetails, Some(new LocalDate("2013-10-10")), Some(BigDecimal(100)), Some("ABCdefgh"), Some("PromABCdefgh"), Some("1234"), true, true, new LocalDate("2015-05-10"), BigDecimal(9324), "1234567891", List(formBundleProp))

      val viewReturnMultipleValues = FormBundleReturn("2014", formBundlePropertyDetails, Some(new LocalDate("2013-10-10")), Some(BigDecimal(100)), Some("ABCdefgh"), Some("PromABCdefgh"), Some("1234"), true, true, new LocalDate("2015-05-10"), BigDecimal(9324), "1234567891", List(formBundleProp, formBundleProp2))

      "show the return view with data when change is allowed" in {

        val changeablePeriod = SubmittedLiabilityReturns(formBundleNo = formBundleNo1,
          description = "",
          liabilityAmount = BigDecimal(432.12),
          dateFrom = new LocalDate("2015-09-08"),
          dateTo = new LocalDate("2015-09-08"),
          dateOfSubmission = new LocalDate("2015-09-08"),
          changeAllowed = true,
          paymentReference = "")

        val chargeableSubmittedReturns = new SubmittedReturns(2015, Nil, List(changeablePeriod))
        val periodSummaryChargeable = PeriodSummaryReturns(periodKey = 2015,
          draftReturns = Nil,
          submittedReturns = Some(new SubmittedReturns(2015, Nil, List(changeablePeriod))))

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

        val chargeableSubmittedReturns = new SubmittedReturns(2015, Nil, List(changeablePeriod))
        val periodSummaryChargeable = PeriodSummaryReturns(periodKey = 2015,
          draftReturns = Nil,
          submittedReturns = Some(new SubmittedReturns(2015, Nil, List(changeablePeriod))))

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
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockFormBundleReturnsService.getFormBundleReturns(Matchers.eq(formBundleNo1))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(formBundleReturn))

    when(mockSummaryReturnsService.getPeriodSummaryReturns(Matchers.eq(2015))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(periodSummaries))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = TestFormBundleReturnController.view(formBundleNo1, 2015).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestFormBundleReturnController.view("12345678901", 2014).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestFormBundleReturnController.view("12345678901", 2014).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestFormBundleReturnController.view("12345678901", 2014).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }
}
