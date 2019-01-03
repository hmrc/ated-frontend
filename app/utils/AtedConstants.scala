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

package utils

import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object AtedConstants {

  val IdentifierArn = "arn"
  val IdentifierUtr = "utr"
  val IdentifierSafeId = "safeid"
  val LoggedInUser = "loggedInUser"
  val SubmitReturnsResponseFormId = "submit-returns-response-Id"
  val SubmitEditedLiabilityReturnsResponseFormId = "submit-edited-liability-returns-response-Id"
  val RetrieveReturnsResponseId = "get-returns-response-Id"
  val PreviousReturnsDetailsList = "previous-return-details-list"
  val RetrieveSubscriptionDataId = "get-subscription-data-response-Id"
  val RetrieveSelectPeriodFormId = "get-selected-period-Id"
  val RetrieveReturnTypeFormId = "get-selected-return-type-Id"
  val DelegatedClientAtedRefNumber = "delegatedClientAtedRefNumber"

  val AddressTypeCorrespondence = "Correspondence"

  val PeriodKey = "2015"
  val PeriodStartMonth = "4"
  val PeriodStartDay = "1"

  val ReliefReturnType = "relief"
  val LiabilityReturnType = "liability"
  val DisposeReturnType = "de-enveloped"

  lazy val FurtherReturnDec =  Messages("ated.edit-liability.declaration.header.further")
  lazy val AmendedReturnDec =  Messages("ated.edit-liability.declaration.header.amend")
  lazy val ChangeDetailsDec =  Messages("ated.edit-liability.declaration.header.change")

  lazy val FurtherReturnSub =  Messages("ated.edit-liability.declaration.submit.further")
  lazy val AmendedReturnSub =  Messages("ated.edit-liability.declaration.submit.amend")
  lazy val ChangedReturnSub =  Messages("ated.edit-liability.declaration.submit.change")

  val TypeReliefDraft = "Relief"
  val TypeLiabilityDraft = "Liability"
  val TypeChangeLiabilityDraft = "Change_Liability"
  val TypeDisposeLiabilityDraft = "Dispose_Liability"

  val Further = "F"
  val Amend = "A"
  val Change = "C"

  val SelectedPreviousReturn = "selected-previous-return"

}
