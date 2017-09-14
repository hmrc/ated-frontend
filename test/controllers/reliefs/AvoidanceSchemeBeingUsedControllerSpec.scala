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

import builders.{AuthBuilder, ReliefBuilder, SessionBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{Reliefs, ReliefsTaxAvoidance, TaxAvoidance}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ReliefsService
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.{AtedConstants, AtedUtils}

import scala.concurrent.Future

class AvoidanceSchemeBeingUsedControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockReliefsService = mock[ReliefsService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val periodKey = 2015

  val testReliefs =  ReliefBuilder.reliefTaxAvoidance(periodKey,Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(false)))
  val testReliefs1 =  ReliefBuilder.reliefTaxAvoidance(periodKey,Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)))
  val testReliefsWithTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey,Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)))
  val testReliefsWithTaxAvoidancePopulated = ReliefBuilder.reliefTaxAvoidance(periodKey,
    Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)),
    TaxAvoidance(rentalBusinessScheme = Some("avoid1"))
  )

  object TestAvoidanceSchemeBeingUsedController extends AvoidanceSchemeBeingUsedController {
    override val authConnector = mockAuthConnector
    override val reliefsService = mockReliefsService
    override val delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector

  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockReliefsService)
    reset(mockBackLinkCache)
  }

  "AvoidanceSchemeBeingUsedController" must {

    implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
    "use correct DelegationConnector" in {
      ChooseReliefsController.delegationConnector must be(FrontendDelegationConnector)
    }

    "view" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/reliefs/2015/avoidance-schemes-used"))
        result.isDefined must be(true)
        status(result.get) must not be (NOT_FOUND)
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

      "Authorised users" must {

        "return a status of OK && Avoidance scheme being used page be displayed empty v2.0" in {
          getAuthorisedUserSome {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))
              document.select(".block-label").text() must include("Yes")
              document.select(".block-label").text() must include("No")          }
        }

      }
      "contain the tax avoidance scheme radio buttons" in {
        getAuthorisedUserNone {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            document.select(".block-label").text() must include("Yes")
            document.select(".block-label").text() must include("No")
            document.getElementById("isAvoidanceScheme-true").attr("checked") must be("")
            document.getElementById("isAvoidanceScheme-false").attr("checked") must be("")
        }
      }

      "show an error if radio button is not selected" in {
        val inputJson = Json.parse( """{"periodKey":2015,"isAvoidanceScheme": "" }""")
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
          result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("There is a problem with the avoidance scheme question")
        }
      }
    }

    "editFromSummary" must {

      "return a status of SEE_OTHER and have the back link set to the summary page when we have no data" in {
        editFromSummary(None) {
          result =>
            status(result) must be(SEE_OTHER)
        }
      }

      "return a status of SEE_OTHER and have the back link set to the summary page" in {
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        editFromSummary(Some(testReliefsWithTaxAvoidance)) {
          result =>
            status(result) must be(SEE_OTHER)
        }
      }
    }

    "submit" must {

      "Authorised users" must {

        "for invalid data, return BAD_REQUEST" in {
          val inputJson = Json.parse( """{"periodKey": 2015,"isAvoidanceScheme": ""}""")
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }
        "for valid data, redirect to Avoidance scheme page" in {
          val inputJson = Json.parse( """{"periodKey": 2015,"isAvoidanceScheme": true}""")
          submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include("/ated/reliefs/2015/avoidance-schemes")

          }
        }
      }
    }
  }

  def getAuthorisedUserNone(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val result = TestAvoidanceSchemeBeingUsedController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getAuthorisedUserSome(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testReliefs)))
    val result = TestAvoidanceSchemeBeingUsedController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getAuthorisedUserWithTaxAvoidanceSome(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testReliefsWithTaxAvoidance)))
    val result = TestAvoidanceSchemeBeingUsedController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getAuthorisedUserWithTaxAvoidancePopulated(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testReliefsWithTaxAvoidancePopulated)))
    val result = TestAvoidanceSchemeBeingUsedController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestAvoidanceSchemeBeingUsedController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestAvoidanceSchemeBeingUsedController.view(periodKey).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.saveDraftIsTaxAvoidance(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testReliefs)))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestAvoidanceSchemeBeingUsedController.send(periodKey).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestAvoidanceSchemeBeingUsedController.send(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }


  def editFromSummary(reliefs: Option[ReliefsTaxAvoidance]= None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(reliefs))
    val result = TestAvoidanceSchemeBeingUsedController.editFromSummary(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestAvoidanceSchemeBeingUsedController.send(periodKey).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

}
