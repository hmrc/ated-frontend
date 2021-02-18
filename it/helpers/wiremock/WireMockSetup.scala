
package helpers.wiremock

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

