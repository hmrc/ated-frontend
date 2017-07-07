/*
 * Copyright 2017 HM Revenue & Customs
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

import builders.AuthBuilder.{createAtedContext, createUserAuthContext}
import builders.{AuthBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.SubmittedReliefReturns
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ReliefsService, SubscriptionDataService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future

class ViewReliefReturnControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val mockReliefsService = mock[ReliefsService]
  val mockSubscriptionDataService = mock[SubscriptionDataService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  val periodKey = 2015
  val formBundleNo = "1234567890"
  val organisationName = "ACME Limited"


  object TestViewReliefReturnController extends ViewReliefReturnController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val reliefsService = mockReliefsService
    override val subscriptionDataService = mockSubscriptionDataService
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockReliefsService)
    reset(mockSubscriptionDataService)
  }


  "ViewReliefReturnController" must {

    "use correct DelegationConnector" in {
      ViewReliefReturnController.delegationConnector must be(FrontendDelegationConnector)
    }

    "use correct ReliefService" in {
      ViewReliefReturnController.reliefsService must be(ReliefsService)
    }

    "use correct SubscriptionDataService" in {
      ViewReliefReturnController.subscriptionDataService must be(SubscriptionDataService)
    }

    "viewReliefReturn" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, s"/ated/view-relief-return/$periodKey/$formBundleNo"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
      }

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
      }

      "Authorised users" must {

        "respond with OK, if summary returns are in cache" in {
          getWithAuthorisedUserSuccess {
            result =>
              status(result) must be(OK)
              val doc = Jsoup.parse(contentAsString(result))
              doc.getElementById("relief-return-subheader").text() must be(organisationName)
              doc.getElementById("relief-return-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              doc.getElementById("relief-return-header").text() must be("View return")
              doc.getElementById("submit").text() must be("Change return")
              doc.title() must be("View return")
          }
        }


        "be redirected to change relief page" in {
          submitWithAuthorisedUser {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must be("/ated/reliefs/2015/change?formBundleNo=")

          }
        }

          "respond with Exception, if summary returns are NOT in cache" in {
            getWithAuthorisedUserFailure {
              result =>
                val thrown = the[RuntimeException] thrownBy await(result)
                thrown.getMessage must be("No reliefs found in the cache for provided period and form bundle id")
            }
          }

      }

    }
  }

  def getWithAuthorisedUserSuccess(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val submittedReturns = SubmittedReliefReturns(formBundleNo, "Property rental businesses", new LocalDate("2015-05-01"), new LocalDate("2015-05-01"), new LocalDate("2015-05-01"), None, None)
    when(mockReliefsService.viewReliefReturn(Matchers.eq(periodKey), Matchers.eq(formBundleNo))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(submittedReturns)))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))

    val result = TestViewReliefReturnController.viewReliefReturn(periodKey, formBundleNo).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithAuthorisedUserFailure(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    when(mockReliefsService.viewReliefReturn(Matchers.eq(periodKey), Matchers.eq(formBundleNo))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(None))
    val result = TestViewReliefReturnController.viewReliefReturn(periodKey, formBundleNo).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestViewReliefReturnController.viewReliefReturn(periodKey, formBundleNo).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestViewReliefReturnController.viewReliefReturn(periodKey, formBundleNo).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }


  def submitWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    when(mockBackLinkCache.clearBackLinks(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Nil))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestViewReliefReturnController.submit(2015, "").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


}
