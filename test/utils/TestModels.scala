/*
 * Copyright 2025 HM Revenue & Customs
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

import java.time.LocalDate
import play.api.libs.json.{JsArray, JsObject, Json}
import utils.AtedConstants._

trait TestModels {
  implicit val mockAppConfig: ApplicationConfig
  val organisationName: String = "OrganisationName"

  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789013"
  val formBundleNo3: String = "876547696786"
  val atedReference: String = "XN1200000100001"
  val cancelAgentUrl: String = ""
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

  val clientMandateDetails: ClientMandateDetails = {
    ClientMandateDetails(
      agentName = "name1",
      changeAgentLink = "",
      email = "aa@a.com",
      changeEmailLink = "",
      status = "Active")
  }
  def draftReturns1(periodKey: Int): DraftReturns = DraftReturns(periodKey, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
  def draftReturns2(periodKey: Int): DraftReturns = DraftReturns(periodKey, "", "some relief", None, TypeReliefDraft)
  def draftReturns2SocialHousing(periodKey: Int): DraftReturns = DraftReturns(periodKey,
    "",
    if (periodKey >= 2020) "Provider of social housing or housing co-operative" else "Social housing",
    None,
    TypeReliefDraft)
  def draftReturns3(periodKey: Int): DraftReturns = DraftReturns(periodKey, "1", "liability draft", Some(BigDecimal(100.00)), TypeLiabilityDraft)
  def draftReturns4(periodKey: Int): DraftReturns = DraftReturns(periodKey, "", "dispose liability draft", None, TypeDisposeLiabilityDraft)

  val submittedReliefReturns1: SubmittedReliefReturns = SubmittedReliefReturns(
    formBundleNo1, "some relief", LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"))
  def submittedReliefReturnsSocialHousing(periodKey: Int): SubmittedReliefReturns = SubmittedReliefReturns(
    formBundleNo1,
    if (periodKey >= 2020) "Provider of social housing or housing co-operative" else "Social housing",
    LocalDate.parse("2015-05-05"),
    LocalDate.parse("2015-05-05"),
    LocalDate.parse("2015-05-05"))
  val submittedLiabilityReturns1: SubmittedLiabilityReturns = SubmittedLiabilityReturns(
    formBundleNo2, "addr1+2", BigDecimal(1234.00), LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"),
    LocalDate.parse("2015-05-05"), changeAllowed = true, "payment-ref-01")

  val liabilityAmount = 123
  val previousReturns: SubmittedLiabilityReturns = SubmittedLiabilityReturns(
    formBundleNo3, "12 Stone Row", BigDecimal(liabilityAmount), LocalDate.parse("2015-05-05"),
    LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"), changeAllowed = false, "payment-ref"
  )

  def submittedReturns(periodKey: Int, withPastReturns: Boolean = false): SubmittedReturns = {
    if (withPastReturns){
      SubmittedReturns(periodKey, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1), Seq(previousReturns))
    }else{
      SubmittedReturns(periodKey, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1), Seq())
    }
  }

  def submittedReturnsSocialHousingModel(periodKey: Int, withPastReturns: Boolean = false): SubmittedReturns = {
    if (withPastReturns){
      SubmittedReturns(periodKey, Seq(submittedReliefReturnsSocialHousing(periodKey)), Seq(submittedLiabilityReturns1), Seq(previousReturns))
    }else{
      SubmittedReturns(periodKey, Seq(submittedReliefReturnsSocialHousing(periodKey)), Seq(submittedLiabilityReturns1), Seq())
    }
  }

  def draftReturnsJson(periodKey: Int): JsArray = Json.arr(
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

  def draftReturnsJsonSocialHousing(periodKey: Int): JsArray = Json.arr(
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
      "description" -> "Social housing",
      "returnType" -> "Relief"
    )
  )

  def submittedReturnsJson(periodKey: Int): JsObject = Json.obj(
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

  def submittedReturnsJsonSocialHousing(periodKey: Int): JsObject = Json.obj(
    "periodKey" -> periodKey,
    "reliefReturns" -> Json.arr(
      Json.obj(
        "formBundleNo" -> formBundleNo1,
        "reliefType" -> "Social housing",
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
                              withSubmittedReturns: Boolean = true, submittedReturnsSocialHousing: Boolean = false): JsObject = {

    val submittedReturnsToUse = if (submittedReturnsSocialHousing) {
      submittedReturnsJsonSocialHousing(periodKey)
    } else {
      submittedReturnsJson(periodKey)
    }

    val draftReturnToUse = if (submittedReturnsSocialHousing) {
      draftReturnsJsonSocialHousing(periodKey)
    } else {
      draftReturnsJson(periodKey)
    }

    if(!withDraftReturns) {
      Json.obj(
        "periodKey" -> periodKey,
        "draftReturns" -> Json.arr(),
        "submittedReturns" -> submittedReturnsToUse
      )
    }else if(!withSubmittedReturns){
      Json.obj(
        "periodKey" -> periodKey,
        "draftReturns" -> draftReturnToUse
      )
    }else{
      Json.obj(
        "periodKey" -> periodKey,
        "draftReturns" -> draftReturnToUse,
        "submittedReturns" -> submittedReturnsToUse
      )
    }
  }

  def allReturnsJson(withDraftReturns: Boolean = true, withSubmittedReturns: Boolean = true,
                     submittedReturnsSocialHousing: Boolean = false): JsObject =
    Json.obj(
      "atedBalance" -> 999.99,
      "allReturns" -> Json.arr(
        periodSummaryReturnJson(currentTaxYear, withDraftReturns, withSubmittedReturns, submittedReturnsSocialHousing),
        periodSummaryReturnJson(currentTaxYear-1, withDraftReturns, withSubmittedReturns, submittedReturnsSocialHousing)
      )
    )

  def periodSummaryReturns(periodKey: Int, withDraftReturns: Boolean = true,
                           withSubmittedReturns: Boolean = true, withPastReturns: Boolean = false,
                           allDraftTypes: Boolean = false, submittedReturnsSocialHousing: Boolean = false): PeriodSummaryReturns = {

    val submittedReturnsToUse = if (submittedReturnsSocialHousing) {
      (submittedReturnsSocialHousingModel(periodKey), submittedReturnsSocialHousingModel(periodKey, withPastReturns))
    } else {
      (submittedReturns(periodKey), submittedReturns(periodKey, withPastReturns))
    }

    val draftReturn2ToUse = if (submittedReturnsSocialHousing) {
      draftReturns2SocialHousing(periodKey)
    } else {
      draftReturns2(periodKey)
    }

    if(!withDraftReturns){
      PeriodSummaryReturns(
        periodKey,
        Nil,
        Some(submittedReturnsToUse._1)
      )
    }else if(!withSubmittedReturns){
      PeriodSummaryReturns(
        periodKey,
        Seq(draftReturns1(periodKey),
          draftReturn2ToUse
        ),
        None
      )
    }else{

      val drafts: Seq[DraftReturns] = if(!allDraftTypes){
        Seq(
          draftReturns1(periodKey),
          draftReturn2ToUse
        )

      }else{
        Seq(
          draftReturns1(periodKey),
          draftReturn2ToUse,
          draftReturns3(periodKey),
          draftReturns4(periodKey)
        )
      }

      PeriodSummaryReturns(
        periodKey,
        drafts,
        Some(submittedReturnsToUse._2)
      )
    }
  }

  def summaryReturnsModel(atedBalance: BigDecimal = BigDecimal(999.99),
                          periodKey: Int,
                          withDraftReturns: Boolean = true,
                          withSubmittedReturns: Boolean = true,
                          withPastReturns: Boolean = false,
                          submittedReturnsSocialHousing: Boolean = false): SummaryReturnsModel = {
    SummaryReturnsModel(
      Some(atedBalance),
      Seq(periodSummaryReturns(periodKey,
        withDraftReturns,
        withSubmittedReturns,
        withPastReturns,
        submittedReturnsSocialHousing = submittedReturnsSocialHousing)),
      Seq(periodSummaryReturns(periodKey - 1,
        withDraftReturns,
        withSubmittedReturns,
        withPastReturns,
        submittedReturnsSocialHousing = submittedReturnsSocialHousing))
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

  val prevReturn: PreviousReturns = PreviousReturns("1 address street", "12345678", LocalDate.parse("2015-04-02"), changeAllowed = true)
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
