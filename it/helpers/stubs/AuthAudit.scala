package helpers.stubs

import helpers.IntegrationBase
import play.api.test.Helpers.OK

trait AuthAudit extends IntegrationBase {

  def stubAuthAudit():Unit = {
    stubPost(url = "/write/audit", status = OK, responseBody = """{"x":2}""")
    stubPost(url = "/write/audit/merged",  status = OK, responseBody = """{"x":2}""")
    stubPost(url = "/auth/authorise", status = OK, responseBody = authResponseJson)
  }

}
