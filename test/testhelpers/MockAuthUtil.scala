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

package testhelpers

import java.util.UUID

import models.{DelegationModel, StandardAuthRetrievals}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import services.DelegationService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.Future

trait MockAuthUtil extends MockitoSugar with TestUtil {

    lazy val userId = "testUserId"
    lazy val mockAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]
    lazy val mockDelegationModel: DelegationModel = mock[DelegationModel]
    lazy val mockDelegationService: DelegationService = mock[DelegationService]

  val agentStandardRetrievals: StandardAuthRetrievals = StandardAuthRetrievals(agentEnrolmentSet, Some(agentAffinity), Some(delegationModel))
  val organisationStandardRetrievals: StandardAuthRetrievals = StandardAuthRetrievals(defaultEnrolmentSet, Some(organisationAffinity), Some(delegationModel))
  val individualStandardRetrievals: StandardAuthRetrievals = StandardAuthRetrievals(defaultEnrolmentSet, Some(individualAffinity), Some(delegationModel))

  def setupAuthForOrganisation(enrolmentSet: Set[Enrolment] = defaultEnrolmentSet) = {
    val authMock = authResultDefault(AffinityGroup.Organisation, enrolmentSet)
    enrolmentSet match {
      case set if set == invalidEnrolmentSet => setInvalidAuthMocks(authMock)
      case _ => setAuthMocks(authMock)
    }
  }

  def authResultDefault(affinityGroup: AffinityGroup, enrolments: Set[Enrolment]): Enrolments ~ Some[AffinityGroup] ~ Some[String] = {
     new ~(
      new ~(
          Enrolments(enrolments),
          Some(affinityGroup)
        ),
      Some(internalId)
    )
  }

  def setAuthMocks(
                      authResult: Enrolments ~ Some[AffinityGroup] ~ Some[String]
                    ): OngoingStubbing[Future[Enrolments ~ Some[AffinityGroup] ~ Some[String]]] = {

    when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))
    when(mockAuthConnector.authorise[Enrolments ~ Some[AffinityGroup] ~ Some[String]]
        (ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(authResult))
  }

  def noDelegationModelAuthMocks(
                    authResult: Enrolments ~ Some[AffinityGroup] ~ Some[String]
                  ): OngoingStubbing[Future[Enrolments ~ Some[AffinityGroup] ~ Some[String]]] = {
    when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockAuthConnector.authorise[Enrolments ~ Some[AffinityGroup] ~ Some[String]]
      (ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(authResult))
  }

  def setInvalidAuthMocks(
                    authResult: Enrolments ~ Some[AffinityGroup] ~ Some[String]
                  ): OngoingStubbing[Future[Enrolments ~ Some[AffinityGroup] ~ Some[String]]] = {

    when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(delegationModel)))
    when(mockAuthConnector.authorise[Enrolments ~ Some[AffinityGroup] ~ Some[String]]
      (ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(InsufficientEnrolments("Auth Error")))
  }

  def setForbiddenAuthMocks(
                    authResult: Enrolments ~ Some[AffinityGroup] ~ Some[String]
                  ): OngoingStubbing[Future[Enrolments ~ Some[AffinityGroup] ~ Some[String]]] = {

    when(mockDelegationService.delegationCall(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockAuthConnector.authorise[Enrolments ~ Some[AffinityGroup] ~ Some[String]]
      (ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(authResult))

  }
}
