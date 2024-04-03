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

package test.helpers.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import play.api.Logging

trait WireMockSetup extends Logging {
  self: WireMockConfig =>

  private val wireMockServer = new WireMockServer(wireMockConfig().port(wireMockPort))

  protected def startWmServer(): Unit = {
    logger.info(s"[startWmServer] - Starting wiremock server on port $wireMockPort")
    wireMockServer.start()
    WireMock.configureFor(wireMockHost, wireMockPort)
  }

  protected def stopWmServer(): Unit = {
    logger.info(s"[startWmServer] - Integration test complete; stopping wiremock server")
    wireMockServer.stop()
  }

  protected def resetWmServer(): Unit = {
    logger.info("[resetWmServer] - Resetting wiremock server for new tests")
    wireMockServer.resetAll()
    WireMock.reset()
  }
}

