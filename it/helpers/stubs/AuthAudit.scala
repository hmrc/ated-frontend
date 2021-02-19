package helpers.stubs

import helpers.IntegrationBase
import play.api.test.Helpers.OK

trait AuthAudit extends IntegrationBase {

  def stubAuthAudit():Unit = {
    stubPost("/write/audit", OK, """{"x":2}""")
    stubPost("/write/audit/merged", OK, """{"x":2}""")
    stubPost("/auth/authorise", 200, authResponseJson)
  }

}
