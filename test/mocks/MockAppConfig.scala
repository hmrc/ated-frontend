/*
 * Copyright 2020 HM Revenue & Customs
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

package mocks

import config.AppConfig
import play.api.{Configuration, Mode}

class MockAppConfig(val runModeConfiguration: Configuration, val mode: Mode = Mode.Test) extends AppConfig {

  override val btaBaseUrl: String = ""
  override val btaHomeUrl: String = "bta-url"
  override val btaHelpAndContactUrl: String = "bta-help-and-contact-url"
  override val btaManageAccountUrl: String = "bta-manage-account-url"
  override val btaMessagesUrl: String = "bta-messages-url"

}