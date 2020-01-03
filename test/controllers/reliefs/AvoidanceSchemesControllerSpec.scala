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

import builders.{ReliefBuilder, SessionBuilder, TitleBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import testhelpers.MockAuthUtil
import models.{Reliefs, ReliefsTaxAvoidance, TaxAvoidance}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ReliefsService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, PeriodUtils}

import scala.concurrent.Future

class AvoidanceSchemesControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with MockAuthUtil {

  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockReliefsSummaryController: ReliefsSummaryController = mock[ReliefsSummaryController]
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]

  val periodKey = 2015
  val testAvoidanceScheme: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey,
    Reliefs(periodKey = periodKey, rentalBusiness = true),
    TaxAvoidance(rentalBusinessScheme = Some("text")))

  class Setup {
    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testAvoidanceSchemesController: AvoidanceSchemesController = new AvoidanceSchemesController(
      mockMcc,
      mockReliefsSummaryController,
      mockAuthAction,
      mockReliefsService,
      mockDataCacheConnector,
      mockBackLinkCacheConnector
    )

    def withAuthorisedUser(testReliefs: Option[ReliefsTaxAvoidance])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)

      when(mockDataCacheConnector.fetchAndGetFormData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))(any(), any(), any()))
        .thenReturn(Future.successful(Some("XN1200000100001")))

      when(mockReliefsService.retrieveDraftReliefs(any(), any())(any(), any()))
        .thenReturn(Future.successful(testReliefs))

      when(mockBackLinkCacheConnector.fetchAndGetBackLink(any())(any()))
        .thenReturn(Future.successful(None))

      when(mockBackLinkCacheConnector.saveBackLink(any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = testAvoidanceSchemesController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def withForbiddenUser(testReliefs: Option[ReliefsTaxAvoidance])(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setForbiddenAuthMocks(authMock)
      when(mockDataCacheConnector.fetchAndGetFormData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))(any(), any(), any()))
        .thenReturn(Future.successful(Some("XN1200000100001")))

      when(mockReliefsService.retrieveDraftReliefs(any(), any())(any(), any()))
        .thenReturn(Future.successful(testReliefs))

      when(mockBackLinkCacheConnector.fetchAndGetBackLink(any())(any()))
        .thenReturn(Future.successful(None))

      when(mockBackLinkCacheConnector.saveBackLink(any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = testAvoidanceSchemesController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def withUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      val result = testAvoidanceSchemesController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithUnAuthorisedUser(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"
      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setInvalidAuthMocks(authMock)
      when(mockBackLinkCacheConnector.saveBackLink(any(), any())(any())).thenReturn(Future.successful(None))
      val result = testAvoidanceSchemesController.submit(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedUser(formData: Seq[(String, String)], avoidanceSchemes: ReliefsTaxAvoidance = testAvoidanceScheme)
                                (test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      when(mockDataCacheConnector.fetchAtedRefData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))
        (any(), any(), any())).thenReturn(Future.successful(Some("XN1200000100001")))

      when(mockReliefsService.retrieveDraftReliefs(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(avoidanceSchemes)))

      when(mockReliefsService.saveDraftTaxAvoidance(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(avoidanceSchemes)))

      when(mockBackLinkCacheConnector.saveBackLink(any(), any())(any()))
        .thenReturn(Future.successful(None))

      val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
      setAuthMocks(authMock)

      val result = testAvoidanceSchemesController.submit(periodKey)
        .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formData: _*), userId))

      test(result)
    }

    def submitForbiddenUser(formData: Seq[(String, String)], avoidanceSchemes: ReliefsTaxAvoidance = testAvoidanceScheme)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"

      when(mockDataCacheConnector.fetchAtedRefData[String](eqTo(AtedConstants.DelegatedClientAtedRefNumber))(any(), any(), any()))
        .thenReturn(Future.successful(Some("XN1200000100001")))

      when(mockReliefsService.retrieveDraftReliefs(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(avoidanceSchemes)))

      when(mockReliefsService.saveDraftTaxAvoidance(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(avoidanceSchemes)))

      when(mockBackLinkCacheConnector.saveBackLink(any(), any())(any()))
        .thenReturn(Future.successful(None))

      val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
      setForbiddenAuthMocks(authMock)

      val result = testAvoidanceSchemesController.submit(periodKey)
        .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formData: _*), userId))

      test(result)
    }
  }

  "TaxAvoidanceController" must {
    "reliefSummary" must {
      "unauthorised users" must {
        "respond with a redirect" in new Setup {
          withUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in new Setup {
          withUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }

        "respond with a redirect to unauthorised URL" in new Setup {
          withForbiddenUser(None) { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {
        "be redirected to the declaration page if we don't have any data" in new Setup {
          withAuthorisedUser(None) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/ated/reliefs/2015/relief-summary")
          }
        }

        "be redirected to the declaration page if we don't have tax avoidance checked" in new Setup {
          withAuthorisedUser(Some(ReliefBuilder.reliefTaxAvoidance(periodKey,
            Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(false))
          ))){ result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/ated/reliefs/2015/relief-summary")
          }
        }

        "show the tax avoidance scheme view" in new Setup {
          withAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(true), openToPublic = true),
              taxAvoidance = TaxAvoidance(openToPublicScheme = Some("12345678"))
            ))) { result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.title() must be(TitleBuilder.buildTitle("Enter your avoidance scheme number"))
              document.getElementById("ated-avoidance-header").text() must be("Enter your avoidance scheme number")
              document.getElementById("relief-summary-text").text() must be("Reliefs claimed")
              document.getElementById("relief-summary-scheme-text").text() must be("Avoidance scheme reference number")
              document.getElementById("relief-summary-scheme-promoter-text").text() must be("Promoter reference number")
              document.getElementById("submit").text() must be("Continue")
          }
        }
      }

      "Submit" must {
        "unauthorised users" must {
          "respond with a redirect" in new Setup {
            submitWithUnAuthorisedUser {
              result =>
                status(result) must be(SEE_OTHER)
            }
          }

          "be redirected to login page" in new Setup {
            submitWithUnAuthorisedUser {
              result =>
                redirectLocation(result).get must include("/ated/unauthorised")
            }
          }

          "respond with a redirect to unauthorised URL" in new Setup {
            val formInput: Seq[(String, String)] = Seq(("rentalBusinessScheme", "12345678"), ("rentalBusinessSchemePromoter", "12345678"))
            submitForbiddenUser(formInput) { result =>
              redirectLocation(result).get must include("/ated/unauthorised")
            }
          }

        }
      }

      "submit" must {
        "authorised user" must {
          "respond with a redirect" in new Setup {
            val formInput: Seq[(String, String)] = Seq(("rentalBusinessScheme", "12345678"), ("rentalBusinessSchemePromoter", "12345678"))

            submitWithAuthorisedUser(formInput) { result =>
              status(result) must be(SEE_OTHER)
            }
          }

          "for invalid data, return BAD_REQUEST" in new Setup {
            val formInput: Seq[Nothing] = Seq()

            when(mockBackLinkCacheConnector.fetchAndGetBackLink(any())(any()))
              .thenReturn(Future.successful(None))

            submitWithAuthorisedUser(formInput) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("You must enter a reference number")
            }
          }

          "if avoidance scheme is not valid, bad request must be returned" in new Setup {
            val formInput: Seq[(String, String)] = Seq(("rentalBusinessScheme", "ABC123"))

            when(mockBackLinkCacheConnector.fetchAndGetBackLink(any())(any()))
              .thenReturn(Future.successful(None))

            submitWithAuthorisedUser(formInput) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("There is a problem with rental business avoidance scheme reference number. Check the number and try again.")
            }
          }

          "display multiple field errors at once" in new Setup {
            val formInput: Seq[(String, String)] = Seq(
              ("rentalBusinessScheme", "12"),
              ("propertyDeveloperScheme", "34"),
              ("lendingScheme", "56")
            )

            val reliefs: ReliefsTaxAvoidance = ReliefsTaxAvoidance(periodKey,
              periodStartDate = PeriodUtils.periodStartDate(periodKey),
              periodEndDate = PeriodUtils.periodEndDate(periodKey),
              reliefs = Reliefs(periodKey = periodKey,rentalBusiness = true, propertyDeveloper = true, lending = true),
              taxAvoidance = TaxAvoidance()
            )

            when(mockBackLinkCacheConnector.fetchAndGetBackLink(any())(any()))
              .thenReturn(Future.successful(None))

            submitWithAuthorisedUser(formInput, reliefs) { result =>
              status(result) must be(BAD_REQUEST)

              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("rentalBusinessScheme-error-0").text() must be("The avoidance scheme number must be 8 digits.")
              document.getElementById("propertyDeveloperScheme-error-0").text() must be("The avoidance scheme number must be 8 digits.")
              document.getElementById("lendingScheme-error-0").text() must be("The avoidance scheme number must be 8 digits.")
            }
          }
        }
      }
    }
  }
}
