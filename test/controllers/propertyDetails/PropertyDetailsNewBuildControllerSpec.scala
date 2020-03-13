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

package controllers.propertyDetails

import java.util.UUID

import builders.{PropertyDetailsBuilder, SessionBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import testhelpers.MockAuthUtil
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants

import scala.concurrent.Future

class PropertyDetailsNewBuildControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil{

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockPropertyDetailsProfessionallyValuedController: PropertyDetailsProfessionallyValuedController = mock[PropertyDetailsProfessionallyValuedController]

  val periodKey: Int = 2016

class Setup {

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  val testPropertyDetailsNewBuildController: PropertyDetailsNewBuildController = new PropertyDetailsNewBuildController(
    mockMcc,
    mockAuthAction,
    mockPropertyDetailsProfessionallyValuedController,
    mockPropertyDetailsService,
    mockDataCacheConnector,
    mockBackLinkCacheConnector
  )

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = testPropertyDetailsNewBuildController.view("1").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getDataWithAuthorisedUser(id: String, propertyDetails: PropertyDetails)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    val result = testPropertyDetailsNewBuildController.view(id).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def saveWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = testPropertyDetailsNewBuildController.save("1", periodKey, None)
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(formBody: List[(String, String)])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockPropertyDetailsService.saveDraftPropertyDetailsNewBuild(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).
      thenReturn(Future.successful(OK))
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val result = testPropertyDetailsNewBuildController.save("1", periodKey, None)
      .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formBody: _*), userId))

    test(result)
  }
}


  override def beforeEach(): Unit = {
  }

  "PropertyDetailsNewBuildController" must {
    "propertyDetails" must {

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

        "show the chargeable property details view if we id and data" in new Setup {
          val propertyDetails: PropertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("postCode"))
          getDataWithAuthorisedUser("1", propertyDetails) {
            result =>
              status(result) must be(OK)
          }
        }
      }
    }

    "save" must {
      "unauthorised users" must {

        "be redirected to the login page" in new Setup {
          saveWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "for invalid data, return BAD_REQUEST" in new Setup {
          val formBody = List(
            ("isNewBuild", "false"))
          when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "When the data is valid forward to the Professionally Valued Page" in new Setup {
          val formBody = List(
            ("isNewBuild", "false"),
            ("notNewBuildDate.day", "1"),
            ("notNewBuildDate.month", "4"),
            ("notNewBuildDate.year", "2016"),
            ("notNewBuildValue", "1500000")
          )
          when(mockBackLinkCacheConnector.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(formBody) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/liability/create/valued/view")
          }
        }
      }
    }
  }
}
