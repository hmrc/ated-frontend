package helpers.stubs

import helpers.IntegrationConstants
import play.api.Application
import play.api.mvc.{Session, DefaultCookieHeaderEncoding, SessionCookieBaker, Cookie}
import uk.gov.hmrc.auth.core.retrieve.{LegacyCredentials, SimpleRetrieval}
import play.api.mvc.{Session, SessionCookieBaker}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import play.api.libs.ws.{DefaultWSCookie, WSCookie}
import uk.gov.hmrc.crypto.PlainText


trait GGLoginStub extends IntegrationConstants {

  val app: Application
  lazy val signerSession: SessionCookieBaker = app.injector.instanceOf[SessionCookieBaker]
  lazy val cookieHeader: DefaultCookieHeaderEncoding = app.injector.instanceOf[DefaultCookieHeaderEncoding]
  lazy val cookieCrypto: SessionCookieCrypto = app.injector.instanceOf[SessionCookieCrypto]

  def encryptedSessionCookie(cookie: Cookie = getSessionCookie): WSCookie =
    encryptedCookie(getSessionCookie)

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

  val cookieData = Map(
      SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
      "userId" -> "/auth/oid/1234567890",
      SimpleRetrieval("authProviderId", LegacyCredentials.reads).toString -> "GGW",
      SessionKeys.authToken -> authToken,
      SessionKeys.sessionId -> sessionId
    )

  def getSessionCookie: Cookie = signerSession.encodeAsCookie(Session(cookieData))
  def getCookieAsHeader: String = cookieHeader.encodeSetCookieHeader(Seq(getSessionCookie))


  // private def cookieData(additionalData: Map[String, String], timeStampRollback: Long): Map[String, String] = {
  //   val timeStamp = new java.util.Date().getTime
  //   val rollbackTimestamp = (timeStamp - timeStampRollback).toString
  //   val lastRequestTimestamp = "ts"

  //   Map(
  //     "sessionId" -> sessionId,
  //     "userId" -> "/auth/oid/1234567890",
  //     "authToken" -> authToken,
  //     SimpleRetrieval("authProviderId", LegacyCredentials.reads).toString -> "GGW",
  //     lastRequestTimestamp -> rollbackTimestamp
  //   ) ++ additionalData
  // }

  // def getCookie(additionalData: Map[String, String] = Map(), timeStampRollback: Long = 0): Cookie =
  //   signerSession.encodeAsCookie(signerSession.deserialize(cookieData(additionalData, timeStampRollback)))

  // def getSessionCookie(additionalData: Map[String, String] = Map(), timeStampRollback: Long = 0): String =
  //   cookieHeader.encodeSetCookieHeader(Seq(getCookie(additionalData, timeStampRollback)))

}
