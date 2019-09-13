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

package controllers.reliefs

import java.util.UUID

import builders.{ReliefBuilder, SessionBuilder, TitleBuilder}
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{Reliefs, ReliefsTaxAvoidance, TaxAvoidance}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DelegationService, ReliefsService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, MockAuthUtil, PeriodUtils}

import scala.concurrent.Future

class AvoidanceSchemesControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val periodKey = 2015
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val testAvoidanceScheme: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey,
    Reliefs(periodKey = periodKey, rentalBusiness = true),
    TaxAvoidance(rentalBusinessScheme = Some("text")))

  object TestTaxAvoidanceController extends AvoidanceSchemesController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    val reliefsService: ReliefsService = mockReliefsService
    override val delegationService: DelegationService = mockDelegationService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockReliefsService)
    reset(mockBackLinkCache)
  }

  "TaxAvoidanceController" must {

    "use correct DelegationService" in {
      AvoidanceSchemesController.delegationService must be(DelegationService)
    }

    "reliefSummary" must {

      "unauthorised users" must {

        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }

        "respond with a redirect to unauthorised URL" in {
          getForbiddenUser(None) { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }
      }

      "Authorised users" must {

        "be redirected to the declaration page if we don't have any data" in {
          getWithAuthorisedUser(None) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/ated/reliefs/2015/relief-summary")
          }
        }


        "be redirected to the declaration page if we don't have tax avoidance checked" in {
          getWithAuthorisedUser(Some(ReliefBuilder.reliefTaxAvoidance(periodKey,
            Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(false))
          ))) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/ated/reliefs/2015/relief-summary")
          }
        }

        "show the tax avoidance scheme view" in {
          getWithAuthorisedUser(Some(
            ReliefBuilder.reliefTaxAvoidance(periodKey,
              Reliefs(periodKey = periodKey, isAvoidanceScheme = Some(true), openToPublic = true),
              taxAvoidance = TaxAvoidance(openToPublicScheme = Some("12345678"))
            ))) {
            result =>
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

        "not respond with NOT_FOUND" in {
          val result = route(FakeRequest(POST, "/ated/reliefs/2015/submit-avoidance-schemes"))
          result.isDefined must be(true)
          status(result.get) must not be NOT_FOUND
        }

        "unauthorised users" must {

          "respond with a redirect" in {
            submitWithUnAuthorisedUser {
              result =>
                status(result) must be(SEE_OTHER)
            }
          }

          "be redirected to login page" in {
            submitWithUnAuthorisedUser {
              result =>
                redirectLocation(result).get must include("/ated/unauthorised")
            }
          }

          "respond with a redirect to unauthorised URL" in {
            val formInput = Seq(("rentalBusinessScheme", "12345678"), ("rentalBusinessSchemePromoter", "12345678"))
            submitForbiddenUser(formInput) { result =>
              redirectLocation(result).get must include("/ated/unauthorised")
            }
          }

        }
      }

      "submit" must {

        "authorised user" must {

          "respond with a redirect" in {
            val formInput = Seq(("rentalBusinessScheme", "12345678"), ("rentalBusinessSchemePromoter", "12345678"))

            submitWithAuthorisedUser(formInput) { result =>
              status(result) must be(SEE_OTHER)
            }
          }

          "for invalid data, return BAD_REQUEST" in {
            val formInput = Seq()
            when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
            submitWithAuthorisedUser(formInput) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("You must enter a reference number")
            }
          }

          "if avoidance scheme is not valid, bad request must be returned" in {
            val formInput = Seq(("rentalBusinessScheme", "ABC123"))
            when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
            submitWithAuthorisedUser(formInput) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("There is a problem with rental business avoidance scheme reference number. Check the number and try again.")
            }
          }

          "display multiple field errors at once" in {
            val formInput = Seq(
              ("rentalBusinessScheme", "12"),
              ("propertyDeveloperScheme", "34"),
              ("lendingScheme", "56")
            )

            val reliefs = ReliefsTaxAvoidance(periodKey,
              periodStartDate = PeriodUtils.periodStartDate(periodKey),
              periodEndDate = PeriodUtils.periodEndDate(periodKey),
              reliefs = Reliefs(periodKey = periodKey,rentalBusiness = true, propertyDeveloper = true, lending = true),
              taxAvoidance = TaxAvoidance()
            )
            when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
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

  def getWithAuthorisedUser(testReliefs: Option[ReliefsTaxAvoidance])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAndGetFormData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReliefs))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestTaxAvoidanceController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getForbiddenUser(testReliefs: Option[ReliefsTaxAvoidance])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setForbiddenAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAndGetFormData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(testReliefs))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestTaxAvoidanceController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestTaxAvoidanceController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestTaxAvoidanceController.submit(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(formData: Seq[(String, String)], avoidanceSchemes: ReliefsTaxAvoidance = testAvoidanceScheme)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(avoidanceSchemes)))
    when(mockReliefsService.saveDraftTaxAvoidance(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(avoidanceSchemes)))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestTaxAvoidanceController.submit(periodKey)
      .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formData: _*), userId))

    test(result)
  }

  def submitForbiddenUser(formData: Seq[(String, String)], avoidanceSchemes: ReliefsTaxAvoidance = testAvoidanceScheme)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(avoidanceSchemes)))
    when(mockReliefsService.saveDraftTaxAvoidance(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(avoidanceSchemes)))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setForbiddenAuthMocks(authMock)
    val result = TestTaxAvoidanceController.submit(periodKey)
      .apply(SessionBuilder.updateRequestFormWithSession(FakeRequest().withFormUrlEncodedBody(formData: _*), userId))

    test(result)
  }

}
