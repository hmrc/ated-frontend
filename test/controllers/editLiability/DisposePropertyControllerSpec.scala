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

import builders._
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DisposeLiabilityReturnService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants
import views.html.BtaNavigationLinks
import views.html.editLiability.dataOfDisposal

import java.util.UUID
import scala.concurrent.Future

class DisposePropertyControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockDisposeLiabilityReturnService: DisposeLiabilityReturnService = mock[DisposeLiabilityReturnService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDisposeLiabilityHasBankDetailsController: DisposeLiabilityHasBankDetailsController = mock[DisposeLiabilityHasBankDetailsController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: dataOfDisposal = app.injector.instanceOf[views.html.editLiability.dataOfDisposal]

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testDisposePropertyController: DisposePropertyController = new DisposePropertyController(
      mockMcc,
      mockDisposeLiabilityReturnService,
      mockAuthAction,
      mockDisposeLiabilityHasBankDetailsController,
      mockServiceInfoService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector,
      injectedViewInstance
    )

    def viewWithAuthorisedUser(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(ArgumentMatchers.eq(oldFormBundleNum))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some("http://backlink")))
      when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
      val result = testDisposePropertyController.view(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def editFromSummary(disposeLiabilityReturn: Option[DisposeLiabilityReturn] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockDisposeLiabilityReturnService.retrieveLiabilityReturn(ArgumentMatchers.eq(oldFormBundleNum))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(disposeLiabilityReturn))
      val result = testDisposePropertyController.editFromSummary(oldFormBundleNum).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithAuthorisedUser(oldFormBundleNum: String, inputJson: JsValue)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      val disposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("123456789012")
      when(mockDisposeLiabilityReturnService.cacheDisposeLiabilityReturnDate(
        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(disposeLiabilityReturn)))
      val result = testDisposePropertyController.save(oldFormBundleNum)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {

    reset(mockDisposeLiabilityReturnService)
    reset(mockDelegationService)
    reset(mockDataCacheConnector)
    reset(mockBackLinkCacheConnector)
    reset(mockDisposeLiabilityHasBankDetailsController)
  }

  lazy val oldFormBundleNum: String = "123456789012"
  lazy val periodKey: Int = 2015

  "DisposePropertyController" must {

    "view" must {

      "return a status of OK, when date of disposal is some(date)" in new Setup {
        val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("12345678901")
        viewWithAuthorisedUser(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("When did you dispose of the property?"))
            assert(document.getElementById("service-info-list").text() === "Home Manage account Messages Help and contact")
            document.getElementsByClass("govuk-caption-xl").text() must be("This section is Change return")
            document.getElementsByClass("hmrc-page-heading").text() must include("When did you dispose of the property?")
            document.getElementById("dateOfDisposal-hint").text() must be("For example, 31 3 2017")
            document.getElementById("submit").text() must be("Save and continue")
            document.getElementsByClass("govuk-back-link").text must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must include("http://backlink")
        }
      }

      "return a status of OK with pre-filled disposeLiabilityForm, when DisposeLiability model is found in the cache" in new Setup {
        val fAddress: FormBundleAddress = FormBundleAddress("line1", "line2", None, None, None, "GB")
        val fProperty: FormBundlePropertyDetails = FormBundlePropertyDetails(None, fAddress, None)
        val fReturn: FormBundleReturn = FormBundleReturn("2015", fProperty, dateOfAcquisition = None, valueAtAcquisition = None,
          taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false,
          dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq())

        val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturn(
          id = "12345678901", fReturn, disposeLiability = Some(DisposeLiability(None, periodKey)))
        viewWithAuthorisedUser(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val doc = Jsoup.parse(contentAsString(result))
            doc.title() must be("When did you dispose of the property? - Submit and view your ATED returns - GOV.UK")
        }
      }

      "return a status of OK with empty form, if DisposeLiability is not found in cache" in new Setup {
        val fAddress: FormBundleAddress = FormBundleAddress("line1", "line2", None, None, None, "GB")
        val fProperty: FormBundlePropertyDetails = FormBundlePropertyDetails(None, fAddress, None)
        val fReturn: FormBundleReturn = FormBundleReturn("2015", fProperty, dateOfAcquisition = None, valueAtAcquisition = None,
          taxAvoidanceScheme = None, localAuthorityCode = None, professionalValuation = true, ninetyDayRuleApplies = false,
          dateOfSubmission = new LocalDate("2015-04-02"), liabilityAmount = BigDecimal(123.45), paymentReference = "payment-ref-123", lineItem = Seq())

        val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturn(id = "12345678901", fReturn)
        viewWithAuthorisedUser(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
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

    "editFromSummary" must {

      "return a status of OK and have the back link set to the summary page" in new Setup {
        val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
          .generateDisposeLiabilityReturn("12345678901")
          .copy(bankDetails = Some(BankDetailsModel(hasBankDetails = false)))
        editFromSummary(Some(disposeLiabilityReturn)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("When did you dispose of the property?"))

            document.getElementsByClass("govuk-back-link").text must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must include("/ated/liability/123456789012/dispose/summary")
        }
      }

      "redirect to account summary page, when that liability return is not-found in cache or ETMP" in new Setup {
        editFromSummary(None) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/account-summary"))
        }
      }
    }

    "save" must {

      "for invalid data - missing day and month, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "", "dateOfDisposal.month": "", "dateOfDisposal.year": "2015", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date of disposal must include the day and month")

        }
      }

      "for invalid date - missing day and year, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "", "dateOfDisposal.month": "12", "dateOfDisposal.year": "", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date of disposal must include the day and year")

        }
      }

      "for invalid data - missing month and year, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "wooooooooow", "dateOfDisposal.month": "", "dateOfDisposal.year": "", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date of disposal must include the month and year")

        }
      }

      "for invalid data - day out of range, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "32", "dateOfDisposal.month": "1", "dateOfDisposal.year": "2015", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a day for date of disposal between 1 and 31")

        }
      }

      "for invalid data - month out of range, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "31", "dateOfDisposal.month": "13", "dateOfDisposal.year": "2015", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Enter a month for date of disposal between 1 and 12")

        }
      }

      "for invalid data - missing day, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "", "dateOfDisposal.month": "12", "dateOfDisposal.year": "2015", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date of disposal must include the day")

        }
      }

      "for invalid data - missing month, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "1", "dateOfDisposal.month": "", "dateOfDisposal.year": "2015", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date of disposal must include the month")

        }
      }

      "for invalid data - missing year, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "1", "dateOfDisposal.month": "1", "dateOfDisposal.year": "", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date of disposal must include the year")

        }
      }

      "for invalid data - year less than 4 digits, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "1", "dateOfDisposal.month": "1", "dateOfDisposal.year": "123", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Year for date of disposal must be 4 digits")

        }
      }

      "for invalid data - non numeric inputs, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "a", "dateOfDisposal.month": "b", "dateOfDisposal.year": "c", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date of disposal must be a valid date")

        }
      }

      "for empty date of disposal, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "", "dateOfDisposal.month": "", "dateOfDisposal.year": "", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Error: When did you dispose of the property?"))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Date of disposal cannot be empty")
        }
      }

      "for invalid date of disposal, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "31", "dateOfDisposal.month": "04", "dateOfDisposal.year": "2015", "periodKey": 2015}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Error: When did you dispose of the property?"))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("Invalid day and month for date of disposal")
        }
      }

      "for valid date of disposal with incorrect period, return BAD_REQUEST" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "31", "dateOfDisposal.month": "5", "dateOfDisposal.year": "2015", "periodKey": 2017}""".stripMargin)
        when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(BAD_REQUEST)
            verify(mockDisposeLiabilityReturnService, times(0))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Error: When did you dispose of the property?"))
            document.getElementsByClass("govuk-error-summary__title").text must include("There is a problem")
            document.getElementsByClass("govuk-list govuk-error-summary__list").text must include("The date of disposal must be in this chargeable period")
        }
      }

      "for valid, redirect to bank details page" in new Setup {
        val inputJson: JsValue = Json.parse(
          """{"dateOfDisposal.day": "30", "dateOfDisposal.month": "6", "dateOfDisposal.year": "2015", "periodKey": 2015}""".stripMargin)
        when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))
        saveWithAuthorisedUser(oldFormBundleNum, inputJson) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/ated/liability/123456789012/dispose/has-bank-details"))
            verify(mockDisposeLiabilityReturnService, times(1))
              .cacheDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }
  }
}
