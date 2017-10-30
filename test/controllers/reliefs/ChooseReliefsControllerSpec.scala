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
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ReliefsService
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.{AtedConstants, AtedUtils}

import scala.concurrent.Future

class ChooseReliefsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {
  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockReliefsService = mock[ReliefsService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val periodKey = 2015

  val reliefsTaxAvoid = ReliefBuilder.reliefTaxAvoidance(periodKey,
    Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = None))
  val testReliefs =  ReliefBuilder.reliefTaxAvoidance(periodKey,Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(false)))
  val testReliefsWithTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey,Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)))
  val testReliefsWithTaxAvoidancePopulated = ReliefBuilder.reliefTaxAvoidance(periodKey,
    Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)),
    TaxAvoidance(rentalBusinessScheme = Some("avoid1"))
  )


  object TestChooseReliefsController extends ChooseReliefsController {
    override val authConnector = mockAuthConnector
    val reliefsService = mockReliefsService
    override protected def delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val controllerId = "controllerId"
    override val backLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockReliefsService)
    reset(mockBackLinkCache)
  }

  "ChooseReliefsController" must {

    implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
    "use correct DelegationConnector" in {
      ChooseReliefsController.delegationConnector must be(FrontendDelegationConnector)
    }

    "view" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/reliefs/2015/choose"))
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

      "Authorised users" must {

          "return a status of OK && Choose reliefs page be displayed empty v2.0" in {
            getAuthorisedUserNone {
              result =>
                status(result) must be(OK)
                val document = Jsoup.parse(contentAsString(result))
                document.getElementById("lede-text").text() must be("You can select more than one relief code. A single relief code can cover one or more properties.")

            }
          }

        "return a status of redirect && Choose reliefs page be displayed filled form, if something has been saved earlier" in {
          getAuthorisedUserSome {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))

              document.getElementById("rentalBusiness").attr("checked") must be("checked")
              document.getElementById("openToPublic").attr("checked") must be("")
              document.getElementById("propertyDeveloper").attr("checked") must be("")
              document.getElementById("propertyTrading").attr("checked") must be("")
              document.getElementById("lending").attr("checked") must be("")
              document.getElementById("employeeOccupation").attr("checked") must be("")
              document.getElementById("farmHouses").attr("checked") must be("")
              document.getElementById("socialHousing").attr("checked") must be("")
          }
        }


        "return a status of OK && Choose reliefs page be displayed filled form, if nothing has been saved earlier" in {
          getAuthorisedUserNone {
            result =>
              status(result) must be(OK)
              val document = Jsoup.parse(contentAsString(result))


              document.getElementById("rentalBusiness").attr("checked") must be("")
              document.getElementById("openToPublic").attr("checked") must be("")
              document.getElementById("propertyDeveloper").attr("checked") must be("")
              document.getElementById("propertyTrading").attr("checked") must be("")
              document.getElementById("lending").attr("checked") must be("")
              document.getElementById("employeeOccupation").attr("checked") must be("")
              document.getElementById("farmHouses").attr("checked") must be("")
              document.getElementById("socialHousing").attr("checked") must be("")

          }
        }
      }

    }

    "editFromSummary" must {

      "return a status of OK and have the back link set to the summary page when we have no data" in {
        editFromSummary(None) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("lede-text").text() must be("You can select more than one relief code. A single relief code can cover one or more properties.")

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/reliefs/2015/relief-summary")
        }
      }

      "return a status of OK and have the back link set to the summary page" in {
        editFromSummary(Some(testReliefs)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("lede-text").text() must be("You can select more than one relief code. A single relief code can cover one or more properties.")

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/reliefs/2015/relief-summary")
        }
      }
    }

    "submit" must {

      "Authorised users" must {

        "for invalid data, return BAD_REQUEST" in {
          val inputJson = Json.parse( """{"periodKey": 2015, "rentalBusiness": false, "isAvoidanceScheme": ""}""")
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody()) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }
        "for invalid data, return BAD_REQUEST v2.0" in {
          val inputJson = Json.parse( """{"periodKey": 2015, "rentalBusiness": false, "isAvoidanceScheme": ""}""")
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody()) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }
 /*       "for respective relief selected, respective dates become mandatory, so give BAD_REQUEST" in {
          val reliefs = Reliefs(periodKey = 2015, rentalBusiness = true, openToPublic = true, propertyDeveloper = true)
          val json = Json.toJson(reliefs)
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withJsonBody(json)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("There is a problem with the rental businesses start date")
          }
        }
        "for all/any dates too early than period, return BAD_REQUEST" in {
          val inputJsonOne = Json.parse(
            """{"periodKey": 2015,
              |"rentalBusiness": true,
              |"rentalBusinessDate.year": "2014",
              |"rentalBusinessDate.month": "05",
              |"rentalBusinessDate.day": "01",
              |"isAvoidanceScheme": true }""".stripMargin)
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJsonOne)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("The rental businesses date must be in this chargeable period")
          }
        }
        "for all/any dates too late than period, return BAD_REQUEST" in {
          val inputJsonOne = Json.parse(
            """{"periodKey": 2015,
              |"rentalBusiness": true,
              |"rentalBusinessDate.year": "2016",
              |"rentalBusinessDate.month": "05",
              |"rentalBusinessDate.day": "01",
              |"isAvoidanceScheme": true }""".stripMargin)
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJsonOne)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("The rental businesses date must be in this chargeable period")
          }
        }
        "for valid data, return OK" in {
          val inputJson = Json.parse( """{"periodKey": 2015, "rentalBusiness": true, "isAvoidanceScheme": true, "rentalBusinessDate.year": "2015", "rentalBusinessDate.month": "05", "rentalBusinessDate.day": "01" }""")
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(SEE_OTHER)
          }
        }*/
      }
    }
  }

  def getAuthorisedUserNone(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val result = TestChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def editFromSummary(reliefs: Option[ReliefsTaxAvoidance]= None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(reliefs))
    val result = TestChooseReliefsController.editFromSummary(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getAuthorisedUserSome(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testReliefs)))
    val result = TestChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getAuthorisedUserWithTaxAvoidanceSome(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testReliefsWithTaxAvoidance)))
    val result = TestChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getAuthorisedUserWithTaxAvoidancePopulated(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testReliefsWithTaxAvoidancePopulated)))
    val result = TestChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getAuthorisedUserSomeAndNoAvoidanceScheme(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(reliefsTaxAvoid)))
    val result = TestChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }


  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.saveDraftReliefs(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(testReliefs)))

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    val result = TestChooseReliefsController.send(periodKey).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

    test(result)
  }

  def submitWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestChooseReliefsController.send(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestChooseReliefsController.send(periodKey).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

}
