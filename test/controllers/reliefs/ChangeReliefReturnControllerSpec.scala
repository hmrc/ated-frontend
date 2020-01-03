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

package controllers.reliefs

import java.util.UUID

import builders.SessionBuilder
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import controllers.propertyDetails.AddressLookupController
import testhelpers.MockAuthUtil
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ReliefsService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants

import scala.concurrent.Future

class ChangeReliefReturnControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {
  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockChooseReliefsController: ChooseReliefsController = mock[ChooseReliefsController]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockAddressLookupController: AddressLookupController = mock[AddressLookupController]

  val periodKey: Int = 2015

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testChangeReliefReturnController: ChangeReliefReturnController = new ChangeReliefReturnController(
    mockMcc,
    mockChooseReliefsController,
    mockAuthAction,
    mockAddressLookupController,
    mockReliefsService,
    mockDataCacheConnector,
    mockBackLinkCacheConnector
  )


  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testChangeReliefReturnController.viewChangeReliefReturn(periodKey, formBundleNumber = "")
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = testChangeReliefReturnController.viewChangeReliefReturn(periodKey, formBundleNumber = "")
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCacheConnector.clearBackLinks(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Nil))
    when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = testChangeReliefReturnController.submit(periodKey, formBundleNumber = "")
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
        "show the change return options" in new Setup {
          getWithAuthorisedUser { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be("Change your ATED return - GOV.UK")
            document.getElementById("relief-return-header").text() must be("Change your ATED return")
          }
        }
      }
    }

    "submit" must {
      "for authorised user" must {
        "with valid form data" must {
          "with invalid form, return BadRequest" in new Setup {
            val inputJson: JsValue = Json.parse( """{"changeRelief": ""}""")
            when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                val doc = Jsoup.parse(contentAsString(result))
                doc.getElementsByClass("error-notification").html() must include("You must select an option")
                contentAsString(result) must include("You must select an option")
            }
          }
          "with changeDetails selected - Redirect to choose relief page" in new Setup {
            val inputJson: JsValue = Json.parse( """{"changeRelief": "changeDetails"}""")
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must be("/ated/reliefs/2015/choose")
            }
          }
          "with createChargeable selected - redirect to address lookup" in new Setup {
            val inputJson: JsValue = Json.parse( """{"changeRelief": "createChargeable"}""")
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
