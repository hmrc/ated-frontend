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

package models

import play.api.Logging
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.ForbiddenException

case class StandardAuthRetrievals(enrolments: Set[Enrolment],
                                  affinityGroup: Option[AffinityGroup],
                                  delegationModel: Option[DelegationModel]) extends Logging {

  private val agentKey = "HMRC-AGENT-AGENT"
  private val saKey = "IR-SA"
  private val atedKey = "HMRC-ATED-ORG"
  private val agentNumberKey = "AgentRefNumber"
  private val atedNumberKey = "ATEDRefNumber"

  private def getEnrolment(key: String): Option[Enrolment] = {
    enrolments.find(_.key.equalsIgnoreCase(key))
  }

  def authLink: String = if (isAgent) agentLink else userLink

  def userLink: String = {
    "/ated/" + atedReferenceNumber
  }

  def agentLink: String = {
    "/ated/" + agentRefNo.getOrElse {
      logger.warn(s"[AtedContext][agentLink] Exception - Agent does not have the correct authorisation")
      throw new RuntimeException("Agent does not have the correct authorisation")
    }
  }

  def agentRefNo: Option[String] = {
    val agentRefNumber = getEnrolment(agentKey)
    val optionRef = if (isAgent) {
      agentRefNumber.flatMap {
        case Enrolment(_, ids, _, _) =>
          val numberOfAgentRefs = ids.count{case EnrolmentIdentifier(k, _) => k.equals(agentNumberKey)}
          logger.info(s"[StandardAuthRetrievals][agentRefNo] Number of Agent Reference numbers: $numberOfAgentRefs")

          ids.collectFirst { case EnrolmentIdentifier(k, v) if k.equals(agentNumberKey) => v }
      }
    } else None
    logger.info(s"[StandardAuthRetrievals][agentRefNo] Agent ref was: $optionRef")
    optionRef
  }

  def isAgent: Boolean = affinityGroup contains Agent

  def isSa: Boolean = getEnrolment(saKey).isDefined

  def clientAtedRefNo: Option[String] = {
    val atedRefNumber = getEnrolment(atedKey)

    val optionRef = atedRefNumber.flatMap {
      case Enrolment(_, ids, _, _) => ids.collectFirst { case EnrolmentIdentifier(k, v) if k.equals(atedNumberKey) => v }
    }
    logger.info(s"[StandardAuthRetrievals][clientAtedRefNo] Client ref was: $optionRef")
    optionRef
  }

  def atedReferenceNumber: String = {
    delegationModel match {
      case Some(delModel) =>
        (isAgent, delModel) match {
          case (true, DelegationModel(_, _, _, taxIdentifiers, _, _)) =>
            logger.info(s"[StandardAuthRetrievals][atedReferenceNumber] Agent with Delegation model ${taxIdentifiers}")
            taxIdentifiers.ated.map(_.value)
          case _ =>
            logger.info(s"[StandardAuthRetrievals][atedReferenceNumber] client with ated ref first case ${clientAtedRefNo}")
            clientAtedRefNo
        }
      case _ =>
        logger.info(s"[StandardAuthRetrievals][atedReferenceNumber] client with ated ref second case ${clientAtedRefNo}")
        clientAtedRefNo
    }
  }.getOrElse {
    logger.warn(s"[StandardAuthRetrieval][atedReferenceNumber] Exception - User forbidden exception")
    throw new ForbiddenException("Forbidden")
  }

}
