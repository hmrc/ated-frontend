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

package test.controllers.reliefs

import play.api.libs.ws.WSResponse
import test.helpers.IntegrationBase
import test.helpers.stubs.{AuthAudit, KeyStore}

class ReliefDeclarationControllerISpec extends IntegrationBase with AuthAudit with KeyStore {

  "respond with a status of SEE OTHER (303)" when {
    "submitting a relief" in {

      stubAuth()
      stubKeyStore()
      stubGet(s"/ated/$atedRef/ated/reliefs/submit/$period", 200, relief)
      val controllerUrl = controllers.reliefs.routes.ReliefDeclarationController.submit(period).url
      val resp: WSResponse = await(client(controllerUrl).withMethod("POST").post(""))

      resp.status mustBe 303
      resp.header("Location") mustBe Some("/ated/reliefs/2019/sent-reliefs")
    }
  }
}