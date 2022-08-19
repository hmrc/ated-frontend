
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

      val controllerUrl = controllers.reliefs.routes.ReliefDeclarationController.submit(period).url
      val sessionCookie = getCookie()
      val resp: WSResponse = await(client(controllerUrl)
        .withCookies(encryptedCookie(sessionCookie))
        .addHttpHeaders(HN.SET_COOKIE -> getSessionCookie())
        //.addHttpHeaders(HeaderNames.xSessionId -> sessionId)
        .post("")
      )

      resp.status mustBe 303
      resp.header("Location") mustBe Some("/ated/reliefs/2019/sent-reliefs")
    }
  }
}