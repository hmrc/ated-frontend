/*
 * Copyright 2019 HM Revenue & Customs
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

///*
// * Copyright 2019 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package builders
//
//import models.StandardAuthRetrievals
//import org.mockito.Matchers
//import org.mockito.Mockito._
//import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
//import uk.gov.hmrc.auth.core.PlayAuthConnector
//import uk.gov.hmrc.domain._
//import uk.gov.hmrc.http.HeaderCarrier
//import uk.gov.hmrc.play.frontend.auth._
//import uk.gov.hmrc.play.frontend.auth.connectors.domain.{AtedAccount, OrgAccount, PayeAccount, _}
//import utils.MockAuthUtil
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//
//object AuthBuilder extends MockAuthUtil {
//
//def createStandardAuthRetrievalsAgent = {
//  val authorised = authResultDefault(Agent)
//  setAuthMocks(authorised)
//}
//
//  def createStandardAuthRetrievalsOrg = {
//    val authorised = authResultDefault(Organisation)
//    setAuthMocks(authorised)
//  }
//
//  def createUserAuthContext(userId: String, userName: String): AuthContext = {
//    AuthContext(authority = createUserAuthority(userId), nameFromSession = Some(userName))
//  }
//
//  def createAgentAuthContext(userId: String, userName: String, agentRefNo: Option[String] = None): AuthContext = {
//    AuthContext(authority = createAgentAuthority(userId, AgentAdmin, agentRefNo), nameFromSession = Some(userName))
//  }
//
//  def createDelegatedAuthContext(userId: String, userName: String): AuthContext = {
//    val user = AuthContext(
//      user = LoggedInUser(
//        userId = userId,
//        loggedInAt = None,
//        previouslyLoggedInAt = None,
//        governmentGatewayToken = None,
//        credentialStrength = CredentialStrength.Weak,
//        confidenceLevel = ConfidenceLevel.L50,
//        oid = ""
//      ),
//      principal = Principal(
//        name = Some(userName),
//        accounts = Accounts(ated = Some(AtedAccount("ated/XN1200000100001", AtedUtr("XN1200000100001"))))),
//        attorney = Some(Attorney(
//        name = userName,
//        returnLink = Link(url = "https://www.tax.service.gov.uk", text = "return"))),
//      userDetailsUri = Some(""),
//      enrolmentsUri = Some(""),
//      idsUri = Some("")
//    )
//    user
//  }
//
//  def createAgentAssistantAuthContext(userId: String, userName: String, agentRefNo: Option[String] = None): AuthContext = {
//    AuthContext(authority = createAgentAuthority(userId, AgentAssistant, agentRefNo), nameFromSession = Some(userName))
//  }
//
//  def createInvalidAuthContext(userId: String, userName: String): AuthContext = {
//    AuthContext(authority = createInvalidAuthority(userId), nameFromSession = Some(userName))
//  }
//
//  def mockAuthorisedUser(userId: String, mockAuthConnector: PlayAuthConnector) {
//    when(mockAuthConnector.authorise(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(
//      Future.successful(Some(createUserAuthority(userId)))
//    )
//  }
//
//  def mockUnsubscribedUser(userId: String, mockAuthConnector: PlayAuthConnector) {
//    when(mockAuthConnector.authorise(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
//      Future.successful(Some(createUnsubscribedUserAuthority(userId)))
//    }
//  }
//
//  def mockAuthorisedAgent(userId: String, mockAuthConnector: PlayAuthConnector) {
//    when(mockAuthConnector.authorise(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
//      Future.successful(Some(createAgentAuthority(userId, AgentAdmin, Some("JARN1234567"))))
//    }
//  }
//
//  def mockUnsubscribedAgent(userId: String, mockAuthConnector: PlayAuthConnector) {
//    when(mockAuthConnector.authorise(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
//      Future.successful(Some(createAgentAuthority(userId, AgentAdmin, None)))
//    }
//  }
//
//  def mockAuthorisedAgentAssistant(userId: String, mockAuthConnector: PlayAuthConnector) {
//    when(mockAuthConnector.authorise(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
//      Future.successful(Some(createAgentAuthority(userId, AgentAssistant, Some("JARN1234567"))))
//    }
//  }
//
//
//  def mockUnAuthorisedUser(userId: String, mockAuthConnector: PlayAuthConnector)(implicit hc: HeaderCarrier) {
//    when(mockAuthConnector.authorise(Matchers.any(), Matchers.any())) thenReturn {
//      val payeAuthority = StandardAuthRetrievals(
//      invalidEnrolmentSet, Some(agentAffinity), credentials.toString, Some(mockDelegationModel)
//      )
//      Future.successful(Some(payeAuthority))
//    }
//  }
//
//  def mockUnAuthorisedUserWithSa(userId: String, mockAuthConnector: PlayAuthConnector) {
//    when(mockAuthConnector.authorise(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn {
//      val saAuthority = Authority(userId, Accounts(sa = Some(SaAccount(userId, SaUtr("1111111111")))),
//        None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
//
//      Future.successful(Some(saAuthority))
//    }
//  }
//
//  private def createInvalidAuthority(userId: String): Authority = {
//    Authority(userId, Accounts(paye = Some(PayeAccount("paye/AA026813", Nino("AA026813B")))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
//  }
//
//  private def createUserAuthority(userId: String): Authority = {
//    Authority(userId, Accounts(org = Some(OrgAccount("org/123", Org("123"))), ated = Some(AtedAccount("ated/XN1200000100001", AtedUtr("XN1200000100001")))),
//      None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
//  }
//
//  private def createUnsubscribedUserAuthority(userId: String): Authority = {
//    Authority(userId, Accounts(org = Some(OrgAccount("org/123", Org("123")))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
//  }
//
//  private def createAgentAuthority(userId: String, agentRole: AgentRole, agentRefNo: Option[String] = None): Authority = {
//    val agentCode = "AGENT-123"
//    val agentBusinessUtr = agentRefNo.map { agentRef =>
//      AgentBusinessUtr(agentRef)
//    }
//
//    val agentAccount = AgentAccount(link = s"agent/$agentCode",
//      agentCode = AgentCode(agentCode),
//      agentUserId = AgentUserId(userId),
//      agentUserRole = agentRole,
//      payeReference = None,
//      agentBusinessUtr = agentBusinessUtr)
//    Authority(userId, Accounts(agent = Some(agentAccount)), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
//  }
//
//}
//
