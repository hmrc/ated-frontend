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

package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder, TitleBuilder}
import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.ReturnType
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
import services.{PropertyDetailsService, ReliefsService}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import utils.AtedConstants

import scala.concurrent.Future

class ExistingReturnQuestionControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAuthConnector = mock[AuthConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockPropertyDetailsService = mock[PropertyDetailsService]
  val mockReliefsService = mock[ReliefsService]
  val returnTypeCharge = "CR"
  val returnTypeRelief = "RR"

  object TestExistingReturnQuestionController extends ExistingReturnQuestionController {
    override val authConnector = mockAuthConnector
    override val delegationConnector: DelegationConnector = FrontendDelegationConnector
    override val propertyDetailsService = mockPropertyDetailsService
    override val reliefsService = mockReliefsService
    override val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBackLinkCache)
  }

  "ReturnTypeController" must {

    "use correct DelegationConnector" in {
      ReturnTypeController.delegationConnector must be(FrontendDelegationConnector)
    }

    "returnType" must {

      "not respond with NOT_FOUND" in {
        val result = route(FakeRequest(GET, "/ated/return-type/2015"))
        result.isDefined must be(true)
        status(result.get) must not be NOT_FOUND
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
        "show the return type view in version 2" in {
          getWithAuthorisedUser { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Does this new return relate to one of your existing returns in the last chargeable period?"))
            document.getElementById("return-type-header").text() must be("Does this new return relate to one of your existing returns in the last chargeable period?")
          }
        }
        "show the return type view with saved data" in {
          getWithAuthorisedUserWithSomeData { result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))
            document.title() must be(TitleBuilder.buildTitle("Does this new return relate to one of your existing returns in the last chargeable period?"))
            document.getElementById("return-type-header").text() must be("Does this new return relate to one of your existing returns in the last chargeable period?")
          }
        }

      }
    }

    "submit" must {
      "for authorised user" must {
        "with valid form data" must {
          "with invalid form, return BadRequest" in {
            val inputJson = Json.parse( """{"yesNo": ""}""")
            when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(BAD_REQUEST)
                val doc = Jsoup.parse(contentAsString(result))
                doc.getElementsByClass("error-notification").html() must include("The existing return question must be answered")
            }
          }
          "with returnType=CR - chargeable return, status is OK" in {
            val inputJson = Json.parse( """{"yesNo": "true"}""")
            submitWithAuthorisedUser(FakeRequest().withJsonBody(inputJson)) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must be("/ated/existing-return/select/2015/CR")
            }
          }

          "with returnType=anything else, status is Redirect" in {
            val inputJson = Json.parse( """{"yesNo": "false"}""")
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

  def getWithAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[String](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestExistingReturnQuestionController.view(2015, returnTypeCharge).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserWithSomeData(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[ReturnType](Matchers.any())
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ReturnType(Some("RR")))))
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestExistingReturnQuestionController.view(2015, returnTypeRelief).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestExistingReturnQuestionController.view(2015, returnTypeCharge).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestExistingReturnQuestionController.view(2015, returnTypeCharge).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def submitWithAuthorisedUser(fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any): Unit = {
    val userId = s"user-${UUID.randomUUID}"
    implicit val user = createAtedContext(createUserAuthContext(userId, "name"))
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockDataCacheConnector.fetchAtedRefData[String](Matchers.eq(AtedConstants.DelegatedClientAtedRefNumber))
      (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockBackLinkCache.clearBackLinks(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Nil))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestExistingReturnQuestionController.submit(2015, returnTypeCharge).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
