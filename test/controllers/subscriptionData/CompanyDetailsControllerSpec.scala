/*
 * Copyright 2018 HM Revenue & Customs
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

import builders.{AuthBuilder, SessionBuilder, TitleBuilder}
import config.FrontendDelegationConnector
import models._
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DetailsService, SubscriptionDataService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class CompanyDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockSubscriptionDataService = mock[SubscriptionDataService]
  val mockDetailsService = mock[DetailsService]


  object TestCompanyDetailsController extends CompanyDetailsController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val subscriptionDataService = mockSubscriptionDataService
    override val detailsDataService: DetailsService = mockDetailsService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSubscriptionDataService)
    reset(mockDetailsService)
  }

  val clientMandateDetails = ClientMandateDetails("agentName", "changeLink", "email", "changeEmailLink")

  "CompanyDetailsController" must {

    "use correct DelegationConnector" in {
      CompanyDetailsController.delegationConnector must be(FrontendDelegationConnector)
    }

    "not respond with NOT_FOUND for the GET" in {
      val result = route(FakeRequest(GET, "/ated/company-details"))
      result.isDefined must be(true)
      status(result.get) must not be(NOT_FOUND)
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

    "Authorised Users" must {

      "return contact details view with editable address" in {
        val addressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val correspondence = Address(Some("name1"), Some("name2"), addressDetails = addressDetails)
        val businessPartnerDetails = RegisteredDetails(true, "testName",
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
            document.getElementById("company-details-header").text() must be("Your ATED details")

            document.getElementById("registered-edit").text() must be("Edit Registered address")
            document.getElementById("registered-edit").attr("href") must be("/ated/registered-details")
        }
      }

      "return contact details view with UR banner" in {
        val addressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val correspondence = Address(Some("name1"), Some("name2"), addressDetails = addressDetails)
        val businessPartnerDetails = RegisteredDetails(true, "testName",
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
            document.getElementById("ur-panel") must not be (null)
            document.getElementById("ur-panel").text() must be("Help improve digital services by joining the HMRC user panel (opens in new window) No thanks")
            document.getElementsByClass("banner-panel__close").text() must be("No thanks")
        }
      }

      "return contact details view with NO editable address" in {
        val addressDetails = AddressDetails(addressType = "", addressLine1 = "", addressLine2 = "", countryCode = "GB")
        val contactDetails = ContactDetails(emailAddress = Some("a@b.c"))
        val correspondence = Address(Some("name1"), Some("name2"), addressDetails = addressDetails, contactDetails = Some(contactDetails))
        val businessPartnerDetails = RegisteredDetails(false, "testName",
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
            document.getElementById("company-details-header").text() must be("Your ATED details")

            document.getElementById("registered-address-label").text() must be("Registered address")
            Option(document.getElementById("registered-edit")) must be(None)
        }
      }

      "Back link redirect to account summary" in {
        getWithAuthorisedUserBack { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "throw exception when missing safeId" in {
        val userId = s"user-${UUID.randomUUID}"
        AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        when(mockSubscriptionDataService.getEmailConsent(Matchers.any(), Matchers.any())).thenReturn(Future.successful(true))
        when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDataService.getRegisteredDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDataService.getSafeId(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDataService.getOverseasCompanyRegistration(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

        val result = TestCompanyDetailsController.view().apply(SessionBuilder.buildRequestWithSession(userId))
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must be("Could not get safeId")
      }
    }
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestCompanyDetailsController.view().apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestCompanyDetailsController.view().apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def getWithAuthorisedUser(correspondence: Option[Address] = None,
                            registeredDetails: Option[RegisteredDetails] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
    when(mockSubscriptionDataService.getEmailConsent(Matchers.any(), Matchers.any())).thenReturn(Future.successful(true))
    when(mockSubscriptionDataService.getCorrespondenceAddress(Matchers.any(), Matchers.any())).thenReturn(Future.successful(correspondence))
    when(mockSubscriptionDataService.getRegisteredDetails(Matchers.any(), Matchers.any())).thenReturn(Future.successful(registeredDetails))
    when(mockSubscriptionDataService.getSafeId(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("safeId")))
    when(mockDetailsService.getClientMandateDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn Future.successful(None)
    when(mockSubscriptionDataService.getOverseasCompanyRegistration(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

    val result = TestCompanyDetailsController.view().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedUserBack(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val result = TestCompanyDetailsController.back().apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }


}
