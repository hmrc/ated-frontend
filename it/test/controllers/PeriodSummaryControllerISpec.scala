/*
 * Copyright 2024 HM Revenue & Customs
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

package test.controllers

import play.api.libs.ws.WSResponse
import test.helpers.IntegrationBase
import test.helpers.stubs.{AuthAudit, KeyStore, ServiceInfoPartialConnectorStub}

class PeriodSummaryControllerISpec extends IntegrationBase with AuthAudit with KeyStore {
  "respond with a status of 200" when {
    "viewing the period summary of 2019" in {
      val period2019 = 2019

      stubAuth()
      ServiceInfoPartialConnectorStub.withResponseForNavLinks()(200, Some(testNavLinkJson))
      stubKeyStore()
      stubGet("/ated/XN1200000100001/returns/partial-summary", 200,
        s"""{
          |"allReturns" : [
          | {
          |   "periodKey" : $period2019,
          |   "draftReturns" : [
          |     {
          |       "periodKey" : $period2019,
          |       "id" : "testId",
          |       "description" : "Social housing",
          |       "returnType" : "Relief"
          |     }
          |   ]
          | }
          |]
          |}""".stripMargin)

      val controllerUrl = controllers.routes.PeriodSummaryController.view(period2019).url

      val resp: WSResponse = await(client(controllerUrl).get())

      resp.status mustBe 200
      resp.body.contains("Social housing") mustBe true
    }

    "viewing the period summary of 2020" in {

      val period2020 = 2020

      stubAuth()
      ServiceInfoPartialConnectorStub.withResponseForNavLinks()(200, Some(testNavLinkJson))
      stubKeyStore()
      stubGet("/ated/XN1200000100001/returns/partial-summary", 200,
        s"""{
          |"allReturns" : [
          | {
          |   "periodKey" : $period2020,
          |   "draftReturns" : [
          |     {
          |       "periodKey" : $period2020,
          |       "id" : "testId",
          |       "description" : "Social housing",
          |       "returnType" : "Relief"
          |     }
          |   ]
          | }
          |]
          |}""".stripMargin)

      val controllerUrl = controllers.routes.PeriodSummaryController.view(period2020).url

      val resp: WSResponse = await(client(controllerUrl).get())

      resp.status mustBe 200
      resp.body.contains("Social housing") mustBe false
      resp.body.contains("Provider of social housing or housing co-operative") mustBe true
    }
  }
}