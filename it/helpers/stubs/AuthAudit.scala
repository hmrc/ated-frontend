package helpers.stubs

import helpers.IntegrationBase
import play.api.test.Helpers.OK

trait AuthAudit extends IntegrationBase {

  def stubAuth(): Unit = {
    stubPost(url = "/auth/authorise", status = OK, responseBody = authResponseJson)
  }

}
