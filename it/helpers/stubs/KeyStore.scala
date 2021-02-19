package helpers.stubs

import helpers.IntegrationBase

trait KeyStore extends IntegrationBase {

  def stubKeyStore():Unit = {
    stubGet(s"/keystore/ated-frontend/$sessionId", 200, keystore)
    stubbedDelete(s"/keystore/ated-frontend/$sessionId", 204)
    stubbedPut(s"/keystore/ated-frontend/$sessionId/data/submit-returns-response-Id", 200)
  }

}
