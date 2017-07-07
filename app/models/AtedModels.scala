/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.LocalDate
import play.api.libs.json.Json


case class EditClientName(atedReferenceNo: String, clientName: String)

object EditClientName {
  implicit val formats = Json.format[EditClientName]
}


case class EditRelief(changeRelief: Option[String] = None)

object EditRelief {
  implicit val formats = Json.format[EditRelief]
}

case class AppointAgent(agentReferenceNumber: String)

object AppointAgent {
  implicit val formats = Json.format[AppointAgent]
}

case class AppointAgentSuccess(name: String, agentReferenceNumber: String)

object AppointAgentSuccess {
  implicit val formats = Json.format[AppointAgentSuccess]
}

case class AgentMatchFailureResponse(reason: String)

object AgentMatchFailureResponse {
  implicit val formats = Json.format[AgentMatchFailureResponse]
}

case class PendingClient(
                          atedReferenceNo: String,
                          clientName: Option[String] = None,
                          expiryDate: Option[LocalDate] = None,
                          clientRejected: Boolean = false
                        )

object PendingClient {
  implicit val formats = Json.format[PendingClient]
}

case class PendingAgent(
                         agentReferenceNo: String,
                         agentName: String,
                         atedReferenceNo: String,
                         rejected: Boolean = false
                       )

object PendingAgent {
  implicit val formats = Json.format[PendingAgent]
}

case class SavePendingClientRequest(
                                     agentReferenceNo: String,
                                     pendingClient: PendingClient,
                                     pendingAgent: PendingAgent
                                   )

object SavePendingClientRequest {
  implicit val formats = Json.format[SavePendingClientRequest]
}

case class ClientsAgent(
                         arn: String,
                         atedRefNo: String,
                         agentName: String,
                         agentRejected: Boolean = false,
                         isEtmpData: Boolean = false
                       )

object ClientsAgent {
  implicit val formats = Json.format[ClientsAgent]
}

case class Client(atedReferenceNo: String, clientName: String)

object Client {
  implicit val formats = Json.format[Client]
}

case class AgentSession(atedReferenceNo: String)

object AgentSession {
  implicit val formats = Json.format[AgentSession]
}

case class ClientDetails(atedReferenceNo: String, name: Option[String] = None)

object ClientDetails {
  implicit val formats = Json.format[ClientDetails]
}

case class ReturnType(returnType: Option[String] = None)// CR = chargeable-return && RR = relief-return

object ReturnType {
  implicit val formats = Json.format[ReturnType]
}

case class SelectPeriod(period: Option[String] = None)
object SelectPeriod {
  implicit val formats = Json.format[SelectPeriod]
}

case class EditLiabilityReturnType(editLiabilityType: Option[String] = None)// ER = edit-return, DP = dispose-property && MP = move-property

object EditLiabilityReturnType {
  implicit val formats = Json.format[EditLiabilityReturnType]
}

case class DisposeLiability(dateOfDisposal: Option[LocalDate] = None, periodKey: Int)

object DisposeLiability {
  implicit val formats = Json.format[DisposeLiability]
}

case class DisposeCalculated(liabilityAmount: BigDecimal, amountDueOrRefund: BigDecimal)

object DisposeCalculated {
  implicit val formats = Json.format[DisposeCalculated]
}

case class DisposeLiabilityReturn(id: String,
                                  formBundleReturn: FormBundleReturn,
                                  disposeLiability: Option[DisposeLiability] = None,
                                  calculated: Option[DisposeCalculated] = None,
                                  bankDetails: Option[BankDetailsModel] = None)

object DisposeLiabilityReturn {
  implicit val formats = Json.format[DisposeLiabilityReturn]
}

case class RemoveClientConfirmation(areYouSure: Option[Boolean])

object RemoveClientConfirmation {
  implicit val formats = Json.format[RemoveClientConfirmation]
}

case class RejectClientConfirmation(areYouSure: Option[Boolean])

object RejectClientConfirmation {
  implicit val formats = Json.format[RejectClientConfirmation]
}

case class ChangeAgent(arn: String, agentName: String)

object ChangeAgent {
  implicit val formats = Json.format[ChangeAgent]
}

case class WantToChangeAgent(wantToChange: Option[Boolean])

object WantToChangeAgent {
  implicit val formats = Json.format[WantToChangeAgent]
}

case class WantToAppointAgent(wantToAppoint: Option[Boolean])

object WantToAppointAgent {
  implicit val formats = Json.format[WantToAppointAgent]
}
