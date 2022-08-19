
package helpers

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.application.IntegrationApplication
import helpers.stubs.GGLoginStub
import helpers.wiremock.WireMockSetup
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSRequest
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderNames
import play.api.libs.ws.WSClient
import play.api.libs.ws.{WSRequest, DefaultWSCookie, WSCookie}
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import play.api.mvc.{Session, SessionCookieBaker}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.crypto.PlainText
import play.api.mvc.Cookie

trait IntegrationBase extends PlaySpec
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with IntegrationApplication
  with WireMockSetup
  with GGLoginStub
  with FutureAwaits
  with DefaultAwaitTimeout
  with IntegrationConstants {

  val sessionId: String
  val authToken: String
  val SessionId: String = sessionId
  val BearerToken: String = authToken
  val headers = List(
    HeaderNames.xSessionId -> SessionId,
    HeaderNames.authorisation -> BearerToken,
    "Csrf-Token" -> "nocheck"
  )
  lazy val client: WSClient = app.injector.instanceOf[WSClient]
  lazy val cookieCrypto: SessionCookieCrypto = app.injector.instanceOf[SessionCookieCrypto]
  lazy val cookieBaker: SessionCookieBaker = app.injector.instanceOf[SessionCookieBaker]

  // def mockSessionCookie: WSCookie = {
  //   val sessionCookie = cookieBaker.encodeAsCookie(Session(Map(
  //     SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
  //     SessionKeys.authToken -> BearerToken,
  //     SessionKeys.sessionId -> SessionId
  //   )))

  //   encryptedCookie(sessionCookie)
  // }

  def encryptedCookie(sessionCookie: Cookie): WSCookie =
    DefaultWSCookie(
      sessionCookie.name,
      cookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value,
      sessionCookie.domain,
      Some(sessionCookie.path),
      sessionCookie.maxAge.map(_.toLong),
      sessionCookie.secure,
      sessionCookie.httpOnly
    )

  def client(path: String): WSRequest = ws.url(s"http://localhost:$port$path")
    .withHttpHeaders(headers:_*)
    .withFollowRedirects(false)

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWmServer()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetWmServer()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopWmServer()
  }

  def stubPost(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(responseBody)
      )
    )

  def stubGet(url: String, status: Integer, body: String): StubMapping =
    stubFor(get(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(body)
      )
    )

  def stubbedDelete(url: String, statusCode: Int): StubMapping = {
    stubFor(delete(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
      )
    )
  }

  def stubbedPut(url: String, statusCode: Int): StubMapping = {
    stubFor(put(urlMatching(url))
      .willReturn(
        aResponse()
          .withStatus(statusCode)
          .withBody(
            """{
              |"id": "xxx",
              |"data": {}
              |}""".stripMargin)
      )
    )
  }
}
