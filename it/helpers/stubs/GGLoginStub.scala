package helpers.stubs

import helpers.IntegrationConstants
import play.api.Application
import play.api.mvc.{Session, DefaultCookieHeaderEncoding, SessionCookieBaker, Cookie}
import uk.gov.hmrc.auth.core.retrieve.{LegacyCredentials, SimpleRetrieval}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import play.api.libs.ws.{DefaultWSCookie, WSCookie}
import uk.gov.hmrc.crypto.PlainText


trait GGLoginStub extends IntegrationConstants {

  val app: Application
  lazy val signerSession: SessionCookieBaker = app.injector.instanceOf[SessionCookieBaker]
  lazy val cookieHeader: DefaultCookieHeaderEncoding = app.injector.instanceOf[DefaultCookieHeaderEncoding]
  lazy val cookieCrypto: SessionCookieCrypto = app.injector.instanceOf[SessionCookieCrypto]
  lazy val cookieData = Map(
    SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
    "userId" -> "/auth/oid/1234567890",
    SimpleRetrieval("authProviderId", LegacyCredentials.reads).toString -> "GGW",
    SessionKeys.authToken -> authToken,
    SessionKeys.sessionId -> sessionId
  )

  def encryptedSessionCookie(cookie: Cookie = getSessionCookie): WSCookie =
    encryptedCookie(getSessionCookie)

  def getSessionCookie: Cookie = signerSession.encodeAsCookie(Session(cookieData))
  def getCookieHeader(sessionCookie: Cookie): String = cookieHeader.encodeSetCookieHeader(Seq(getSessionCookie))

  private def encryptedCookie(sessionCookie: Cookie): WSCookie =
    DefaultWSCookie(
      sessionCookie.name,
      cookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value,
      sessionCookie.domain,
      Some(sessionCookie.path),
      sessionCookie.maxAge.map(_.toLong),
      sessionCookie.secure,
      sessionCookie.httpOnly
    )

}
