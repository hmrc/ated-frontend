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

package test.helpers.stubs

import test.helpers.IntegrationBase

trait KeyStore extends IntegrationBase {

  def stubKeyStore():Unit = {
    stubGet(s"/keystore/ated-frontend/$sessionId", 200, keystore)
    stubbedDelete(s"/keystore/ated-frontend/$sessionId", 204)
    stubbedPut(s"/keystore/ated-frontend/$sessionId/data/submit-returns-response-Id", 200)
  }

}
