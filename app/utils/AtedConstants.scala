/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

object AtedConstants {

  val IdentifierArn: String = "arn"
  val IdentifierUtr: String = "utr"
  val IdentifierSafeId: String = "safeid"
  val LoggedInUser: String = "loggedInUser"
  val SubmitReturnsResponseFormId: String = "submit-returns-response-Id"
  val SubmitEditedLiabilityReturnsResponseFormId: String = "submit-edited-liability-returns-response-Id"
  val RetrieveReturnsResponseId: String = "get-returns-response-Id"
  val PreviousReturnsDetailsList: String = "previous-return-details-list"
  val RetrieveSubscriptionDataId: String = "get-subscription-data-response-Id"
  val RetrieveSelectPeriodFormId: String = "get-selected-period-Id"
  val RetrieveReturnTypeFormId: String = "get-selected-return-type-Id"
  val DelegatedClientAtedRefNumber: String = "delegatedClientAtedRefNumber"

  val AddressTypeCorrespondence: String = "Correspondence"

  val PeriodKey: String = "2015"
  val PeriodStartMonth: String = "4"
  val PeriodStartDay: String = "1"

  val ReliefReturnType: String = "relief"
  val LiabilityReturnType: String = "liability"
  val DisposeReturnType: String = "de-enveloped"

  lazy val FurtherReturnDec: String =  "ated.edit-liability.declaration.header.further"
  lazy val AmendedReturnDec: String =  "ated.edit-liability.declaration.header.amend"
  lazy val ChangeDetailsDec: String =  "ated.edit-liability.declaration.header.change"

  lazy val FurtherReturnSub: String =  "ated.edit-liability.declaration.submit.further"
  lazy val AmendedReturnSub: String =  "ated.edit-liability.declaration.submit.amend"
  lazy val ChangedReturnSub: String =  "ated.edit-liability.declaration.submit.change"

  val TypeReliefDraft: String = "Relief"
  val TypeLiabilityDraft: String = "Liability"
  val TypeChangeLiabilityDraft: String = "Change_Liability"
  val TypeDisposeLiabilityDraft: String = "Dispose_Liability"

  val Further: String = "F"
  val Amend: String = "A"
  val Change: String = "C"

  val SelectedPreviousReturn: String = "selected-previous-return"

  val draftType: String = "ated.draft"
  val submittedType: String = "ated.submitted"

}
