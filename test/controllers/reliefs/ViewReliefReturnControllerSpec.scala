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

import builders.SessionBuilder
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.SubmittedReliefReturns
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{DelegationService, ReliefsService, SubscriptionDataService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class ViewReliefReturnControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockSubscriptionDataService: SubscriptionDataService = mock[SubscriptionDataService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val periodKey = 2015
  val formBundleNo = "1234567890"
  val organisationName = "ACME Limited"


  object TestViewReliefReturnController extends ViewReliefReturnController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService
    override val reliefsService: ReliefsService = mockReliefsService
    override val subscriptionDataService: SubscriptionDataService = mockSubscriptionDataService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockReliefsService)
    reset(mockSubscriptionDataService)
  }


  "ViewReliefReturnController" must {

    "use correct DelegationConnector" in {
      ViewReliefReturnController.delegationService must be(DelegationService)
    }

    "use correct ReliefService" in {
      ViewReliefReturnController.reliefsService must be(ReliefsService)
    }

    "use correct SubscriptionDataService" in {
      ViewReliefReturnController.subscriptionDataService must be(SubscriptionDataService)
    }

    "viewReliefReturn" must {

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
              doc.getElementById("relief-return-subheader").text() must be("This section is: " + organisationName)
              doc.getElementById("relief-return-text").text() must be("For the ATED period from 1 April 2015 to 31 March 2016.")
              doc.getElementById("relief-return-header").text() must be("View return")
              doc.getElementById("submit").text() must be("Change return")
              doc.title() must be("View return - GOV.UK")
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
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val submittedReturns = SubmittedReliefReturns(
      formBundleNo, "Property rental businesses", new LocalDate("2015-05-01"), new LocalDate("2015-05-01"), new LocalDate("2015-05-01"), None, None)
    when(mockReliefsService.viewReliefReturn(Matchers.eq(periodKey), Matchers.eq(formBundleNo))(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(submittedReturns)))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    val result = TestViewReliefReturnController.viewReliefReturn(periodKey, formBundleNo).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserFailure(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
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
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestViewReliefReturnController.viewReliefReturn(periodKey, formBundleNo).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(test: Future[Result] => Any): Unit = {
    val periodKey: Int = 2015
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockSubscriptionDataService.getOrganisationName(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(organisationName)))
    when(mockBackLinkCache.clearBackLinks(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Nil))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestViewReliefReturnController.submit(periodKey, "").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
}
