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

package controllers.propertyDetails

import java.util.UUID

import builders.{SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import testhelpers.MockAuthUtil
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants

import scala.concurrent.Future

class SelectExistingReturnAddressControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockSummaryReturnsService: SummaryReturnsService = mock[SummaryReturnsService]
  val mockPropertyDetailsAddressController: PropertyDetailsAddressController = mock[PropertyDetailsAddressController]
  val mockFormBundleReturnsService: FormBundleReturnsService = mock[FormBundleReturnsService]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]

  val returnTypeCharge: String = "CR"
  val returnTypeRelief: String = "RR"

  class Setup {

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testSelectExistingReturnAddressController: SelectExistingReturnAddressController = new SelectExistingReturnAddressController(
      mockMcc,
      mockAuthAction,
      mockSummaryReturnsService,
      mockPropertyDetailsAddressController,
      mockFormBundleReturnsService,
      mockPropertyDetailsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector
    )

    def viewWithUnAuthorisedUser(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val prevReturns = Seq(PreviousReturns("1, addressLine1", "12345678"))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockSummaryReturnsService.getPreviousSubmittedLiabilityDetails
      (ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(prevReturns))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testSelectExistingReturnAddressController.view(periodKey, returnTypeCharge).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def viewWithAuthorisedUser(prevReturns: Option[Seq[PreviousReturns]])(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockSummaryReturnsService.retrieveCachedPreviousReturnAddressList(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(prevReturns))
      when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testSelectExistingReturnAddressController.view(periodKey, returnTypeCharge).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
      val periodKey: Int = 2015
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val prevReturns = Some(Seq(PreviousReturns("1, addressLine1", "12345678")))
      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockSummaryReturnsService.retrieveCachedPreviousReturnAddressList(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(prevReturns))
      val result = testSelectExistingReturnAddressController.continue(periodKey, returnTypeCharge).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithAuthorisedUser(formBundleReturn: Option[FormBundleReturn],
                               prevReturns: Option[Seq[PreviousReturns]],
                               inputJson: JsValue)(test: Future[Result] => Any) {
      val periodKey: Int = 2014
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)

      when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
      when(mockSummaryReturnsService.retrieveCachedPreviousReturnAddressList(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(prevReturns))
      when(mockFormBundleReturnsService.getFormBundleReturns
      (ArgumentMatchers.eq("12345678"))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(formBundleReturn))
      when(mockDataCacheConnector.saveFormData[Boolean]
        (ArgumentMatchers.eq(AtedConstants.SelectedPreviousReturn), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
      when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
      val result = testSelectExistingReturnAddressController.continue(periodKey, returnTypeCharge)
        .apply(SessionBuilder.updateRequestWithSession(FakeRequest().withJsonBody(inputJson), userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
  }

  "SelectExistingReturnAddressController" must {
    val prevReturns = Some(Seq(PreviousReturns("1, addressLine1", "12345678")))
    "view" must {
      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          viewWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "show the address details view if address list is retrieved from cache" in new Setup {
          val prevReturns: Seq[PreviousReturns] = Seq(PreviousReturns("1, addressLine1", "12345678"))
          viewWithAuthorisedUser(Some(prevReturns)) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be (TitleBuilder.buildTitle("Select the previous return this new return relates to"))
          }
        }

        "show the address details view with no addresses if address list is not retrieved from cache" in new Setup {
          viewWithAuthorisedUser(None) {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be (TitleBuilder.buildTitle("Select the previous return this new return relates to"))
          }
        }

      }
    }

    "save" must {
      "unauthorised users" must {

        "respond with a redirect" in new Setup {
          saveWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }


      "submitting an invalid request should fail and return to the search results page" in new Setup {
        saveWithAuthorisedUser(None, prevReturns, Json.toJson(AddressSelected(None))) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be (TitleBuilder.buildTitle("Select the previous return this new return relates to"))
        }
      }

      "submitting an invalid request should fail and return to the search results page even with cached data" in new Setup {
        saveWithAuthorisedUser(None, prevReturns, Json.toJson(AddressSelected(None))) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select the previous return this new return relates to"))

        }
      }

      "submitting an invalid request should fail and return to the search results page even with no cached data" in new Setup {
        saveWithAuthorisedUser(None, None, Json.toJson(AddressSelected(None))) {
          result =>
            status(result) must be(BAD_REQUEST)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Select the previous return this new return relates to"))

        }
      }

      "submitting an valid request should get the form bundle return and save in keystore" in new Setup {
        val formBundleProp = FormBundleProperty(BigDecimal(100), new LocalDate("2015-09-08"),
          new LocalDate("2015-10-12"), "Relief", Some("Property developers"))
        val formBundleProp2 = FormBundleProperty(BigDecimal(200), new LocalDate("2015-10-12"),
          new LocalDate("2015-12-12"), "Relief", Some("Property developers"))
        val formBundleAddress = FormBundleAddress("1 addressLine1", "addressLine2", Some("addressLine3"), Some("AddressLine4"), Some("XX11XX"), "GB")
        val formBundlePropertyDetails = FormBundlePropertyDetails(Some("title here"), formBundleAddress, Some("additional details"))
        val viewReturn = FormBundleReturn("2014", formBundlePropertyDetails,
          Some(new LocalDate("2013-10-10")),
          Some(BigDecimal(100)),
          Some("ABCdefgh"),
          Some("PromABCdefgh"),
          Some("1234"), true, true, new LocalDate("2015-05-10"), BigDecimal(9324), "1234567891", List(formBundleProp))

        saveWithAuthorisedUser(Some(viewReturn), prevReturns, Json.toJson(AddressSelected(Some("12345678")))) {
          result =>
            status(result) must be(SEE_OTHER)
        }
      }

      "submitting an invalid form bundle number request should redirect to Account Summary Page" in new Setup {
        saveWithAuthorisedUser(formBundleReturn = None, prevReturns, Json.toJson(AddressSelected(Some("12345678")))) {
          result =>
            status(result) must be(SEE_OTHER)
        }
      }
    }
  }
 }
