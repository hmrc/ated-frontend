/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import connectors.DelegationConnector
import models.{DelegationModel, Link, PrincipalTaxIdentifiers}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class DelegationServiceSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with GuiceOneAppPerSuite with Injecting {

  val mockDelegationConnector: DelegationConnector = mock[DelegationConnector]
  implicit val ec: ExecutionContext = inject[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  class Setup {
    val testDelegationService: DelegationService = new DelegationService(
      mockDelegationConnector
    )
  }

  override def beforeEach(): Unit = {
    reset(mockDelegationConnector)
  }

  "delegationCall" should {
    "return a delegation model" when {
      "http response is returned" in new Setup {

        val returnJson = Json.parse(
          """{
            |"attorneyName" : "Attorney",
            |"principalName" : "Principal",
            |"link" : {
            | "text": "String",
            | "url": "String"
            |},
            |"accounts" : {},
            |"supplementaryData" : {},
            |"internalId" : "test"
            |}
            |"""
            .stripMargin)

        val expectedModel = DelegationModel(
          attorneyName = "Attorney",
          principalName = "Principal",
          link = Link(
            text = "String",
            url = "String"
          ),
          accounts = PrincipalTaxIdentifiers(),
          supplementaryData = Some(Map()),
          internalId = Some("test")
        )
        when(mockDelegationConnector.delegationDataCall(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, returnJson.toString)))
        val result: Future[Option[DelegationModel]] = testDelegationService.delegationCall("String")
        await(result) mustBe Some(expectedModel)
      }
    }

    "not return a delegation model" when {
      "no http response is returned" in new Setup {

        when(mockDelegationConnector.delegationDataCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "")))
        val result: Future[Option[DelegationModel]] = testDelegationService.delegationCall("String")
        await(result) mustBe None
      }
    }
  }
}
