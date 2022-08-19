package helpers.stubs

import helpers.IntegrationConstants
import play.api.Application
import play.api.mvc.{DefaultCookieHeaderEncoding, DefaultSessionCookieBaker, Cookie}
import uk.gov.hmrc.auth.core.retrieve.{LegacyCredentials, SimpleRetrieval}

trait GGLoginStub extends IntegrationConstants {

  val app: Application
  lazy val signerSession: DefaultSessionCookieBaker = app.injector.instanceOf[DefaultSessionCookieBaker]
  lazy val cookieHeader: DefaultCookieHeaderEncoding = app.injector.instanceOf[DefaultCookieHeaderEncoding]

  private def cookieData(additionalData: Map[String, String], timeStampRollback: Long): Map[String, String] = {
    val timeStamp = new java.util.Date().getTime
    val rollbackTimestamp = (timeStamp - timeStampRollback).toString
    val lastRequestTimestamp = "ts"

    Map(
      "sessionId" -> sessionId,
      "userId" -> "/auth/oid/1234567890",
      "authToken" -> authToken,
      SimpleRetrieval("authProviderId", LegacyCredentials.reads).toString -> "GGW",
      lastRequestTimestamp -> rollbackTimestamp
    ) ++ additionalData
  }

  def getCookie(additionalData: Map[String, String] = Map(), timeStampRollback: Long = 0): Cookie =
    signerSession.encodeAsCookie(signerSession.deserialize(cookieData(additionalData, timeStampRollback)))

  def getSessionCookie(additionalData: Map[String, String] = Map(), timeStampRollback: Long = 0): String =
    cookieHeader.encodeSetCookieHeader(Seq(getCookie(additionalData, timeStampRollback)))

}
