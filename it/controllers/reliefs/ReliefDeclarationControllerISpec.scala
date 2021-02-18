
package controllers.reliefs

import helpers.IntegrationBase
import helpers.stubs.{AuthAudit, KeyStore}
import play.api.http.{HeaderNames => HN}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HeaderNames

class ReliefDeclarationControllerISpec extends IntegrationBase with AuthAudit with KeyStore {

  "respond with a status of SEE OTHER (303)" when {
    "submitting a relief" in {

      stubAuthAudit()
      stubKeyStore()
      stubGet(s"/ated/$atedRef/ated/reliefs/submit/$period", 200, relief)
      stubPost(s"/annual-tax-enveloped-dwellings/returns/$atedRef", 200, relief)
      stubGet(s"/annual-tax-enveloped-dwellings/subscription/$atedRef", 200, subscription)
      stubGet("/user-details/id/602e38a42d00005ca6358f36", 200, userDetails)

      val controllerUrl = controllers.reliefs.routes.ReliefDeclarationController.submit(period).url

      val resp: WSResponse = await(client(controllerUrl)
        .withHttpHeaders(HN.SET_COOKIE -> getSessionCookie())
        .addHttpHeaders(HeaderNames.xSessionId -> sessionId)
        .post("")
      )

      resp.status mustBe 303
      resp.header("Location") mustBe Some("/ated/reliefs/2019/sent-reliefs")
    }
  }
}