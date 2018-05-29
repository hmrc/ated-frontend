/*
 * Copyright 2018 HM Revenue & Customs
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

package models

import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.domain.{AgentBusinessUtr, AtedUtr}
import uk.gov.hmrc.http.{InternalServerException, UnauthorizedException}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.AgentAdmin

case class AtedContext(request: Request[AnyContent], user: AtedUser)

case class AtedUser(authContext: AuthContext) {

  private def accounts = authContext.principal.accounts

  def name: String = authContext.principal.name.getOrElse("")

  def authLink: String = {
    if (isAgent) agentLink
    else userLink
  }

  def userLink: String = {
    accounts.ated.map(_.link).getOrElse(throw new UnauthorizedException("User does not have the correct authorisation"))
  }

  def agentLink: String = {
    accounts.agent.map(_.link).getOrElse {
      Logger.warn(s"[AuthUtils][getAgentLink] Exception - User does not have the correct authorisation ")
      throw new RuntimeException("User does not have the correct authorisation")
    }
  }

  def isAgent: Boolean = accounts.agent.isDefined

  def isAgentAdmin: Boolean = accounts.agent.exists(_.agentUserRole.satisfiesRequiredRole(AgentAdmin))

  def agentReferenceNo = {
    accounts.agent.flatMap(_.agentBusinessUtr) match {
      case Some(x) => x.value
      case _ =>
        Logger.warn(s"[AuthUtils][agentReferenceNo] Exception - No Agent Reference Defined for this agent")
        throw new RuntimeException("No Agent Reference Defined for this agent")
    }
  }

  def atedReferenceNumber: String = {
    isAgent match {
      case true =>
        Logger.warn(s"AgentRefNumber:" + authContext.principal.accounts.agent.get.agentBusinessUtr)
        Logger.warn(s"AtedUtr:" + authContext.principal.accounts.ated.get.utr)
        throw new RuntimeException("Agents don't have ated ref no")
      case false =>
        accounts.ated.map(_.utr.utr).getOrElse(throw new RuntimeException("No Ated Reference Defined for this user"))
    }
  }

  def clientId: String = {
    isAgent match {
      case false => atedReferenceNumber
      case _ => ""
    }
  }
}
