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

package controllers.subscriptionData

import java.util.UUID
import builders.{SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.AuthAction
import models._
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
import services.{DetailsService, ServiceInfoService, SubscriptionDataService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.BtaNavigationLinks
import views.html.subcriptionData.companyDetails

import scala.concurrent.Future

class CompanyDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockDetailsService: DetailsService = mock[DetailsService]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: companyDetails = app.injector.instanceOf[views.html.subcriptionData.companyDetails]

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testCompanyDetailsController: CompanyDetailsController = new CompanyDetailsController(
      mockMcc,
      mockAuthAction,
      mockSubscriptionDataService,
      mockServiceInfoService,
      mockDetailsService,
    injectedViewInstance
  )

  def getWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = testCompanyDetailsController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(correspondence: Option[Address] = None,
                            registeredDetails: Option[RegisteredDetails] = None)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages,mockAppConfig)))
    when(mockSubscriptionDataService.getEmailConsent(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
    when(mockSubscriptionDataService.getCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(correspondence))
    when(mockSubscriptionDataService.getRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(registeredDetails))
    when(mockSubscriptionDataService.getSafeId(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("safeId")))
    when(mockDetailsService.getClientMandateDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(None))
    when(mockSubscriptionDataService.getOverseasCompanyRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = testCompanyDetailsController.view().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedUserBack(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)

    val result = testCompanyDetailsController.back().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }
}

  override def beforeEach(): Unit = {
  }

  val clientMandateDetails: ClientMandateDetails = ClientMandateDetails("agentName", "changeLink", "email", "changeEmailLink")

  "CompanyDetailsController" must {
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

    "Authorised Users" must {

      "return contact details view with editable address" in new Setup {
        val addressDetails: AddressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val correspondence: Address = Address(Some("name1"), Some("name2"), addressDetails = addressDetails)
        val businessPartnerDetails: RegisteredDetails = RegisteredDetails(isEditable = true, "testName",
          RegisteredAddressDetails(addressLine1 = "bpline1",
            addressLine2 = "bpline2",
            addressLine3 = Some("bpline3"),
            addressLine4 = Some("bpline4"),
            postalCode = Some("postCode"),
            countryCode = "GB"))

        getWithAuthorisedUser(Some(correspondence), Some(businessPartnerDetails)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be (TitleBuilder.buildTitle("Your ATED details"))
            document.getElementsByTag("h1").text() must include("Your ATED details")
            document.getElementById("registered-edit").text() must be("Change Registered address")
            document.getElementById("registered-edit").attr("href") must be("/ated/registered-details")

            document.getElementsByClass("govuk-back-link").text() must be("Back")
            document.getElementsByClass("govuk-back-link").attr("href") must be("/ated/account-summary")
        }
      }

      "return contact details view with UR banner" in new Setup {
        val addressDetails: AddressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val correspondence: Address = Address(Some("name1"), Some("name2"), addressDetails = addressDetails)
        val businessPartnerDetails: RegisteredDetails = RegisteredDetails(isEditable = true, "testName",
          RegisteredAddressDetails(addressLine1 = "bpline1",
            addressLine2 = "bpline2",
            addressLine3 = Some("bpline3"),
            addressLine4 = Some("bpline4"),
            postalCode = Some("postCode"),
            countryCode = "GB"))

        getWithAuthorisedUser(Some(correspondence), Some(businessPartnerDetails)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be(TitleBuilder.buildTitle("Your ATED details"))
            document.select("div.hmrc-user-research-banner") must not be null
            document.select("div.hmrc-user-research-banner").text() must include("Help make GOV.UK better")
            document.getElementsByClass("hmrc-user-research-banner__close").text() must include("Hide message. I do not want to take part in research")
        }
      }

      "return contact details view with NO editable address" in new Setup {
        val addressDetails: AddressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val contactDetails: ContactDetails = ContactDetails(emailAddress = Some("a@b.c"))
        val correspondence: Address = Address(Some("name1"), Some("name2"), addressDetails = addressDetails, contactDetails = Some(contactDetails))
        val businessPartnerDetails: RegisteredDetails = RegisteredDetails(isEditable = false, "testName",
          RegisteredAddressDetails(addressLine1 = "bpline1",
            addressLine2 = "bpline2",
            addressLine3 = Some("bpline3"),
            addressLine4 = Some("bpline4"),
            postalCode = Some("postCode"),
            countryCode = "GB"))

        getWithAuthorisedUser(Some(correspondence), Some(businessPartnerDetails)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be (TitleBuilder.buildTitle("Your ATED details"))
            document.getElementsByTag("h1").text() must include("Your ATED details")

            document.select("dt.govuk-summary-list__key").get(2).text() must be("Registered address")
            document.select("registered-edit").size() mustBe 0
        }
      }

      "Back link redirect to account summary" in new Setup {
        getWithAuthorisedUserBack { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "throw exception when missing safeId" in new Setup {
        val userId = s"user-${UUID.randomUUID}"
        val authMock: Enrolments ~ Some[AffinityGroup] ~ Some[String] = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
        setAuthMocks(authMock)
        when(mockSubscriptionDataService.getEmailConsent(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(true))
        when(mockSubscriptionDataService.getCorrespondenceAddress(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDataService.getRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDataService.getSafeId(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDataService.getOverseasCompanyRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

        val result: Future[Result] = testCompanyDetailsController.view().apply(SessionBuilder.buildRequestWithSession(userId))
        val thrown: RuntimeException = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must be("Could not get safeId")
      }
    }
  }
}
