/*
 * Copyright 2023 HM Revenue & Customs
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

package builders

import java.util.UUID

import play.api.mvc.request.RequestTarget
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson}
import play.api.test.FakeRequest

object SessionBuilder {

  val TOKEN = "token"

  def updateRequestWithSession(fakeRequest: FakeRequest[AnyContentAsJson], userId: String): FakeRequest[AnyContentAsJson] = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(
      "sessionId" -> sessionId,
      TOKEN -> "RANDOMTOKEN",
      "userId" -> userId)
  }


  def updateRequestFormWithSession(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], userId: String): FakeRequest[AnyContentAsFormUrlEncoded] = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(
      "sessionId" -> sessionId,
      TOKEN -> "RANDOMTOKEN",
      "userId" -> userId)
  }

  def buildRequestWithSession(userId: String, queryParams: Option[(String, Seq[String])] = None) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val fr = FakeRequest().withSession(
      "sessionId" -> sessionId,
      TOKEN -> "RANDOMTOKEN",
      "userId" -> userId).withTarget(RequestTarget("", "", Map(queryParams.getOrElse("dummy" -> Seq()))))
    fr
  }

  def buildRequestWithSessionDelegation(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId,
      TOKEN -> "RANDOMTOKEN",
      "delegationState" -> "On",
      "userId" -> userId)
  }

  def buildRequestWithSessionNoUser = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId)
  }
}
