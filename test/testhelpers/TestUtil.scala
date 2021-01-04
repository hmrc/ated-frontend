/*
 * Copyright 2021 HM Revenue & Customs
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

package testhelpers

import models.{DelegationModel, Link, PrincipalTaxIdentifiers}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.domain.AtedUtr

trait TestUtil {

  val invalidEnrolmentSet: Set[Enrolment] = Set(Enrolment("HMRC-TEST-ORG", Seq(EnrolmentIdentifier("TestRefNumber", "XN1200000100001")), "activated"),
    Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "6543210")), "activated"))

  val defaultEnrolmentSet: Set[Enrolment] = Set(Enrolment("HMRC-ATED-ORG", Seq(EnrolmentIdentifier("ATEDRefNumber", "XN1200000100001")), "activated"),
    Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "6543210")), "activated"), Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "0123456")), "activated"))

  val agentEnrolmentSet: Set[Enrolment] = Set(Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", "JARN1234567")), "activated"),
    Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "6543210")), "activated"), Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "0123456")), "activated"))

  val saEnrolmentSet: Set[Enrolment] = Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "0123456")), "activated"))


  val defaultSaEnrolmentSet: Set[Enrolment] = Set(Enrolment("HMRC-ATED-ORG", Seq(EnrolmentIdentifier("ATEDRefNumber", "XN1200000100001")), "activated"),
    Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "0123456")), "activated"))

  val atedOnlyEnrolmentSet: Set[Enrolment] = Set(Enrolment("HMRC-ATED-ORG", Seq(EnrolmentIdentifier("ATEDRefNumber", "XN1200000100001")), "activated"))

  val agentOnlyEnrolmentSet: Set[Enrolment] = Set(Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", "JARN1234567")), "activated"))

  val irSaCtEnrolmentSet: Set[Enrolment] =
    Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "6543210")), "activated"),
      Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "0123456")), "activated"))

  val irSaAtedEnrolmentSet: Set[Enrolment] = Set(Enrolment("HMRC-ATED-ORG", Seq(EnrolmentIdentifier("ATEDRefNumber", "XN1200000100001")), "activated"),
    Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "0123456")), "activated"))

  val atedAgentEnrolmentSet: Set[Enrolment] = Set(Enrolment("HMRC-ATED-ORG", Seq(EnrolmentIdentifier("ATEDRefNumber", "XN1200000100001")), "activated"),
    Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", "JARN1234567")), "activated"))


  val internalId = "Id"

  val individualAffinity: AffinityGroup = AffinityGroup.Individual
  val organisationAffinity: AffinityGroup = AffinityGroup.Organisation
  val agentAffinity: AffinityGroup = AffinityGroup.Agent


  val link: Link = Link("url", "text")
  val principalTaxIdentifiers: PrincipalTaxIdentifiers = PrincipalTaxIdentifiers(None, None, None, None, None, None, Some(AtedUtr("XN1200000100001")))
  val delegationModel: DelegationModel = DelegationModel("attorney", "principalName", link, accounts = principalTaxIdentifiers, None, Some("Id"))

}
