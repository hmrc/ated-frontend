/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.helpers.stubs

import play.api.Application
import play.api.mvc.{Session, DefaultCookieHeaderEncoding, SessionCookieBaker, Cookie}
import uk.gov.hmrc.auth.core.retrieve.{LegacyCredentials, SimpleRetrieval}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import play.api.libs.ws.{DefaultWSCookie, WSCookie}
import test.helpers.IntegrationConstants
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
