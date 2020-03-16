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

import config.ApplicationConfig
import models._
import org.joda.time.LocalDate
import play.api.libs.json.{JsObject, Json}
import utils.AtedConstants.{AddressTypeCorrespondence, TypeChangeLiabilityDraft, TypeDisposeLiabilityDraft, TypeLiabilityDraft, TypeReliefDraft}

trait TestModels {
  implicit val mockAppConfig: ApplicationConfig
  val organisationName: String = "OrganisationName"

  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789013"
  val formBundleNo3: String = "876547696786"

  val currentYear: Int = LocalDate.now().getYear
  lazy val currentTaxYear: Int = PeriodUtils.calculatePeakStartYear(LocalDate.now())

  val address: Address = {
    Address(name1 = Some("name1"),
      name2 = Some("name2"),
      contactDetails = Some(ContactDetails(phoneNumber = Some("03000123456789"),
        mobileNumber = Some("09876543211"),
        emailAddress = Some("aa@aa.com"),
        faxNumber = Some("0223344556677"))),
      addressDetails = AddressDetails(AddressTypeCorrespondence, "addrLine1", "addrLine2", None, None, None, "GB"))}

  def draftReturns1(periodKey: Int) = DraftReturns(periodKey, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
  def draftReturns2(periodKey: Int) = DraftReturns(periodKey, "", "some relief", None, TypeReliefDraft)
  def draftReturns3(periodKey: Int) = DraftReturns(periodKey, "1", "liability draft", Some(BigDecimal(100.00)), TypeLiabilityDraft)
  def draftReturns4(periodKey: Int) = DraftReturns(periodKey, "", "dispose liability draft", None, TypeDisposeLiabilityDraft)

  val submittedReliefReturns1 = SubmittedReliefReturns(
    formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
  val submittedLiabilityReturns1 = SubmittedLiabilityReturns(
    formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"),
    new LocalDate("2015-05-05"), changeAllowed = true, "payment-ref-01")

  val previousReturns = SubmittedLiabilityReturns(
    formBundleNo3, "12 Stone Row", BigDecimal(123), new LocalDate("2015-05-05"),
    new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), changeAllowed = false, "payment-ref"
  )

  def submittedReturns(periodKey: Int, withPastReturns: Boolean = false) =

    if(withPastReturns){
      SubmittedReturns(periodKey, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1), Seq(previousReturns))
    }else{
      SubmittedReturns(periodKey, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1), Seq())
    }

  def draftReturnsJson(periodKey: Int) = Json.arr(
    Json.obj(
      "periodKey" -> periodKey,
      "id" -> "1",
      "description" -> "desc",
      "charge" -> 100.0,
      "returnType" -> "Change_Liability"
    ),
    Json.obj(
      "periodKey" -> periodKey,
      "id" -> "",
      "description" -> "some relief",
      "returnType" -> "Relief"
    )
  )

  def submittedReturnsJson(periodKey: Int) = Json.obj(
    "periodKey" -> periodKey,
    "reliefReturns" -> Json.arr(
      Json.obj(
        "formBundleNo" -> formBundleNo1,
        "reliefType" -> "some relief",
        "dateFrom" -> "2015-05-05",
        "dateTo" -> "2015-05-05",
        "dateOfSubmission" -> "2015-05-05"
      )
    ),
    "oldLiabilityReturns" -> Json.arr(),
    "currentLiabilityReturns" -> Json.arr(
      Json.obj(
        "formBundleNo" -> formBundleNo2,
        "description" -> "addr1+2",
        "liabilityAmount" -> 1234.0,
        "dateFrom" -> "2015-05-05",
        "dateTo" -> "2015-05-05",
        "dateOfSubmission" -> "2015-05-05",
        "changeAllowed" -> true,
        "paymentReference" -> "payment-ref-01"
      )
    )
  )

  def periodSummaryReturnJson(periodKey: Int, withDraftReturns: Boolean = true,
                              withSubmittedReturns: Boolean = true): JsObject = {

    if(!withDraftReturns) {
      Json.obj(
        "periodKey" -> periodKey,
        "draftReturns" -> Json.arr(),
        "submittedReturns" -> submittedReturnsJson(periodKey)
      )
    }else if(!withSubmittedReturns){
      Json.obj(
        "periodKey" -> periodKey,
        "draftReturns" -> draftReturnsJson(periodKey)
      )
    }else{
      Json.obj(
        "periodKey" -> periodKey,
        "draftReturns" -> draftReturnsJson(periodKey),
        "submittedReturns" -> submittedReturnsJson(periodKey)
      )
    }
  }

  def allReturnsJson(withDraftReturns: Boolean = true, withSubmittedReturns: Boolean = true): JsObject =
    Json.obj(
      "atedBalance" -> 999.99,
      "allReturns" -> Json.arr(
        periodSummaryReturnJson(currentTaxYear, withDraftReturns, withSubmittedReturns),
        periodSummaryReturnJson(currentTaxYear-1, withDraftReturns, withSubmittedReturns)
      )
    )

  def periodSummaryReturns(periodKey: Int, withDraftReturns: Boolean = true,
                           withSubmittedReturns: Boolean = true, withPastReturns: Boolean = false,
                           allDraftTypes: Boolean = false): PeriodSummaryReturns = {

    if(!withDraftReturns){
      PeriodSummaryReturns(
        periodKey,
        Nil,
        Some(submittedReturns(periodKey))
      )
    }else if(!withSubmittedReturns){
      PeriodSummaryReturns(
        periodKey,
        Seq(draftReturns1(periodKey),
          draftReturns2(periodKey)
        ),
        None
      )
    }else{

      val drafts: Seq[DraftReturns] = if(!allDraftTypes){
        Seq(
          draftReturns1(periodKey),
          draftReturns2(periodKey)
        )

      }else{
        Seq(
          draftReturns1(periodKey),
          draftReturns2(periodKey),
          draftReturns3(periodKey),
          draftReturns4(periodKey)
        )
      }

      PeriodSummaryReturns(
        periodKey,
        drafts,
        Some(submittedReturns(periodKey, withPastReturns))
      )
    }
  }

  def summaryReturnsModel(atedBalance: BigDecimal = BigDecimal(999.99),
                          periodKey: Int, withDraftReturns: Boolean = true,
                          withSubmittedReturns: Boolean = true, withPastReturns: Boolean = false): SummaryReturnsModel = {
    SummaryReturnsModel(
      Some(atedBalance),
      Seq(periodSummaryReturns(periodKey, withDraftReturns, withSubmittedReturns, withPastReturns)),
      Seq(periodSummaryReturns(periodKey - 1, withDraftReturns, withSubmittedReturns, withPastReturns))
    )
  }

  def summaryReturnsModelCurrentOnly(atedBalance: BigDecimal = BigDecimal(999.99),
                          periodKey: Int, withDraftReturns: Boolean = true,
                          withSubmittedReturns: Boolean = true, withPastReturns: Boolean = false): SummaryReturnsModel = {
    SummaryReturnsModel(
      Some(atedBalance),
      Seq(periodSummaryReturns(periodKey, withDraftReturns, withSubmittedReturns, withPastReturns))
    )
  }

  val prevReturn = PreviousReturns("1 address street", "12345678", new LocalDate("2015-04-02"))
  val pastReturnDetails: Seq[PreviousReturns] = Seq(prevReturn)

  def currentYearReturnsForDisplay: Seq[AccountSummaryRowModel] = {
    Seq(
      AccountSummaryRowModel(
        returnType = "ated.draft",
        description = "Change_Liability",
        route = "example/draft/route"
      ),
      AccountSummaryRowModel(
        returnType = "ated.submitted",
        description = "19 Stone Row",
        formBundleNo = Some("12345678"),
        route = "example/non-draft/route"
      )
    )
  }
}
