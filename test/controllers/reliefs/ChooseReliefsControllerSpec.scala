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

import builders.{ReliefBuilder, SessionBuilder}
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{Reliefs, ReliefsTaxAvoidance, TaxAvoidance}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{DelegationService, ReliefsService}
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class ChooseReliefsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val periodKey = 2015
  val mockReliefsService: ReliefsService = mock[ReliefsService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val reliefsTaxAvoid: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey,
    Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = None))
  val testReliefs: ReliefsTaxAvoidance =
    ReliefBuilder.reliefTaxAvoidance(periodKey,Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(false)))
  val testReliefsWithTaxAvoidance: ReliefsTaxAvoidance =
    ReliefBuilder.reliefTaxAvoidance(periodKey,Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)))
  val testReliefsWithTaxAvoidancePopulated: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey,
    Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true)),
    TaxAvoidance(rentalBusinessScheme = Some("avoid1"))
  )


  object TestChooseReliefsController extends ChooseReliefsController {
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

  "ChooseReliefsController" must {

    implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
    "use correct DelegationService" in {
      ChooseReliefsController.delegationService must be(DelegationService)
    }

    "view" must {

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
                document.getElementById("lede-text")
                  .text() must be("You can select more than one relief code. A single relief code can cover one or more properties.")

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
            document.getElementById("lede-text")
              .text() must be("You can select more than one relief code. A single relief code can cover one or more properties.")

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/reliefs/2015/relief-summary")
        }
      }

      "return a status of OK and have the back link set to the summary page" in {
        editFromSummary(Some(testReliefs)) {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.getElementById("lede-text")
              .text() must be("You can select more than one relief code. A single relief code can cover one or more properties.")

            document.getElementById("backLinkHref").text must be("Back")
            document.getElementById("backLinkHref").attr("href") must include("/ated/reliefs/2015/relief-summary")
        }
      }

      "respond with a redirect to unauthorised URL" in {
        forbiddenEditFromSummary(Some(testReliefs)) { result =>
          redirectLocation(result).get must include("/ated/unauthorised")
        }
      }
    }

    "submit" must {

      "Authorised users" must {

        "respond with a redirect to unauthorised URL for forbidden user" in {
          val formBody = List(
            ("periodKey", "2015"),
            ("rentalBusiness", "true"),
            ("isAvoidanceScheme", "true"),
            ("rentalBusinessDate.year", "2015"),
            ("rentalBusinessDate.month", "05"),
            ("rentalBusinessDate.day", "01"))
          forbiddenSubmitUser(FakeRequest().withFormUrlEncodedBody(formBody: _*)) { result =>
            redirectLocation(result).get must include("/ated/unauthorised")
          }
        }

        "for invalid data, return BAD_REQUEST" in {
          val inputJson = Json.parse( """{"periodKey": 2015, "rentalBusiness": false, "isAvoidanceScheme": ""}""")
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), inputJson) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for invalid data, return BAD_REQUEST v2.0" in {
          val inputJson = Json.parse( """{"periodKey": 2015, "rentalBusiness": false, "isAvoidanceScheme": ""}""")
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), inputJson) {
            result =>
              status(result) must be(BAD_REQUEST)
          }
        }

        "for respective relief selected, respective dates become mandatory, so give BAD_REQUEST" in {
            val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, openToPublic = true, propertyDeveloper = true)
            val json = Json.toJson(reliefs)
            when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), json) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("There is a problem with the page")
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
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), inputJsonOne) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("There is a problem with the page")
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
            submitWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(), inputJsonOne) {
              result =>
                status(result) must be(BAD_REQUEST)
                contentAsString(result) must include("There is a problem with the page")
            }
          }
          "for valid data, return OK" in {
            val formBody = List(
              ("periodKey", "2015"),
              ("rentalBusiness", "true"),
              ("isAvoidanceScheme", "true"),
              ("rentalBusinessDate.year", "2015"),
              ("rentalBusinessDate.month", "05"),
              ("rentalBusinessDate.day", "01"))
            when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
            submitFormBodyWithAuthorisedUser(FakeRequest().withFormUrlEncodedBody(formBody: _*)) {
              result =>
                status(result) must be(SEE_OTHER)
            }
          }
        }
    }
  }

  def getAuthorisedUserNone(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
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
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(reliefs))
    val result = TestChooseReliefsController.editFromSummary(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def forbiddenEditFromSummary(reliefs: Option[ReliefsTaxAvoidance]= None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setForbiddenAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(reliefs))
    val result = TestChooseReliefsController.editFromSummary(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getAuthorisedUserSome(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockReliefsService.retrieveDraftReliefs(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(testReliefs)))
    val result = TestChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    val result = TestChooseReliefsController.view(periodKey).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], inputJson: JsValue)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.saveDraftReliefs(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(testReliefs)))

    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestChooseReliefsController.send(periodKey).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

    test(result)
  }

  def submitFormBodyWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.saveDraftReliefs(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(testReliefs)))

    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    val result = TestChooseReliefsController.send(periodKey).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

    test(result)
  }

  def forbiddenSubmitUser(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockReliefsService.saveDraftReliefs(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(Some(testReliefs)))

    val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setForbiddenAuthMocks(authMock)
    val result = TestChooseReliefsController.send(periodKey).apply(SessionBuilder.updateRequestFormWithSession(fakeRequest, userId))

    test(result)
  }
}
