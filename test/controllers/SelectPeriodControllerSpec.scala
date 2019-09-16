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

package controllers

import java.util.UUID

import builders.{SessionBuilder, TitleBuilder}
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.SelectPeriod
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DelegationService
import uk.gov.hmrc.auth.core.{AffinityGroup, PlayAuthConnector}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AtedConstants, MockAuthUtil}

import scala.concurrent.Future

class SelectPeriodControllerSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with MockAuthUtil {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  object TestSelectPeriodController extends SelectPeriodController {
    override val authConnector: PlayAuthConnector = mockAuthConnector
    override val delegationService: DelegationService = mockDelegationService
    override val controllerId: String = "controllerId"
    override val backLinkCacheConnector: BackLinkCacheConnector = mockBackLinkCache
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  "SelectPeriodController" should {
    "use correct delegationService" in {
        SelectPeriodController.delegationService mustBe DelegationService
    }
    "use correct controllerId" in {
      SelectPeriodController.controllerId mustBe "SelectPeriodController"
    }
    "use correct backLinkCacheConnector" in {
      SelectPeriodController.backLinkCacheConnector mustBe BackLinkCacheConnector
    }
    "use correct dataCacheConnector" in {
      SelectPeriodController.dataCacheConnector mustBe DataCacheConnector
    }

  }

  "selectPeriod" must {

    "unauthorised users" must {

      "respond with a redirect" in {
        getWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

    }

    "be redirected to the unauthorised page" in {
      getWithUnAuthorisedUser { result =>
        redirectLocation(result).get must include("/ated/unauthorised")
      }
    }

    "Authorised users" must {

      "show the select period view" in {
        getWithAuthorisedUser { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("Select an ATED chargeable period"))
          document.getElementById("header").text() must include("Select an ATED chargeable period")
          document.getElementById("details-text").text() must be("The chargeable period for a year runs from the 1 April to 31 March.")
          document.getElementById("period-2015_field").text() must be("2015 to 2016")
          document.getElementById("period-2016_field").text() must be("2016 to 2017")
          document.getElementById("period-2017_field").text() must be("2017 to 2018")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "show the select period view with data if we have some" in {
        getWithAuthorisedUserWithSavedData { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(TitleBuilder.buildTitle("Select an ATED chargeable period"))
          document.getElementById("header").text() must include("Select an ATED chargeable period")
          document.getElementById("details-text").text() must be("The chargeable period for a year runs from the 1 April to 31 March.")
          document.getElementById("period-2015_field").text() must be("2015 to 2016")
          document.getElementById("period-2015").attr("checked") must be("checked")
          document.getElementById("period-2016_field").text() must be("2016 to 2017")
          document.getElementById("period-2017_field").text() must be("2017 to 2018")
          document.getElementById("submit").text() must be("Continue")
        }
      }

    }

    "submit" must {

      "for authorised user" must {

        "with invalid form, return BadRequest" in {
          val inputJson = Json.parse( """{"returnType": ""}""")
          submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson), Some("XN1200000100001")) {
            result =>
              status(result) must be(BAD_REQUEST)
              val doc = Jsoup.parse(contentAsString(result))
              doc.getElementsByClass("error-notification").html() must include("Select an option for type of return")
              contentAsString(result) must include("Select an option for type of return")
          }
        }

        "with period=2015 Redirect to select return type page" in {
          val inputJson = Json.parse( """{"period": "2015"}""")
          submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson), Some("XN1200000100001")) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must be("/ated/return-type/2015")
          }
        }

        "with period=2016 - Redirect to select return type page" in {
          val inputJson = Json.parse( """{"period": "2016"}""")
          submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson), Some("XN1200000100001")) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must be("/ated/return-type/2016")
          }
        }

        "with period=2017 - Redirect to select return type page" in {
          val inputJson = Json.parse( """{"period": "2017"}""")
          submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson), Some("XN1200000100001")) {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must be("/ated/return-type/2017")
          }
        }

        "redirect to error page, if clients do not match" in {
          val inputJson = Json.parse( """{"period": "2016"}""")
          submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
            result =>
              val document = Jsoup.parse(contentAsString(result))
              status(result) must be(OK)
              document.getElementById("content").text() must include
              "There are one or more people from your organisation signed in with the same Government Gateway details"
          }
        }
      }

    }
  }

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))

    when(mockDataCacheConnector.fetchAndGetFormData[String](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestSelectPeriodController.view.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }
  def getWithAuthorisedUserWithSavedData(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))

    when(mockDataCacheConnector.fetchAndGetFormData[SelectPeriod](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(SelectPeriod(Some("2015")))))

    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestSelectPeriodController.view.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
        val authMock = authResultDefault(AffinityGroup.Organisation, invalidEnrolmentSet)
    setInvalidAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    val result = TestSelectPeriodController.view.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson], atedRef: Option[String] = None)(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    val authMock = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
    setAuthMocks(authMock)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(atedRef))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestSelectPeriodController.submit.apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
