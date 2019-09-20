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

package services

import connectors.DelegationConnector
import models.{DelegationModel, Link, PrincipalTaxIdentifiers}
import org.mockito.Matchers
import org.mockito.Mockito.{reset, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DelegationServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockDelegationConnector: DelegationConnector = mock[DelegationConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  object TestDelegationService extends DelegationService {

    override val delegationConnector: DelegationConnector = mockDelegationConnector
  }

  override def beforeEach: Unit = {
    reset(mockDelegationConnector)
  }

  "delegationCall" should {
    "return a delegation model" when {
      "http response is returned" in {

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
        when(mockDelegationConnector.delegationDataCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(returnJson))))
        val result = TestDelegationService.delegationCall("String")
        await(result) shouldBe Some(expectedModel)
      }
    }

    "not return a delegation model" when {
      "no http response is returned" in {

        when(mockDelegationConnector.delegationDataCall(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
        val result = TestDelegationService.delegationCall("String")
        await(result) shouldBe None
      }
    }
  }
}
