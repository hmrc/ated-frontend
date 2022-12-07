/*
 * Copyright 2022 HM Revenue & Customs
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

package forms

import config.ApplicationConfig
import forms.ReliefForms._
import models._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.data.{Form, FormError}
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest


class ReliefFormsSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val noPromoterErrorMessage = "ated.avoidance-schemes.promoter.empty"
  val noSchemeErrorMessage = "ated.avoidance-schemes.scheme.empty"
  val periodKey: Int = 2019
  val periodKey2021: Int = 2021
  val maxChars: Long = 102400

  "validateTaxAvoidance" must {
    "fail if we have no data" in {

      val emptyTaxAvoidance = TaxAvoidance()
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance),periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.errors.head.key must be ("")
      result.errors.head.message must be ("ated.avoidance-schemes.scheme.empty")
    }

    "fail with only 1 error if we have a rental scheme and empty public Scheme" in {
      val emptyTaxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("12345678"), openToPublicScheme = Some(""))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("rentalBusinessSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.rentalBusinessSchemePromoter"))
    }

    "fail if we have only have the rental scheme" in {
      val emptyTaxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("rentalBusinessSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.rentalBusinessSchemePromoter"))
    }

    "fail if we have only have the openToPublicScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(openToPublicScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("openToPublicSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.openToPublicSchemePromoter"))
    }

    "fail if we have only have the propertyDeveloperScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(propertyDeveloperScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("propertyDeveloperSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.propertyDeveloperSchemePromoter"))
    }

    "fail if we have only have the propertyTradingScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(propertyTradingScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("propertyTradingSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.propertyTradingSchemePromoter"))
    }

    "fail if we have only have the lendingScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(lendingScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("lendingSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.lendingSchemePromoter"))
    }

    "fail if we have only have the employeeOccupationScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(employeeOccupationScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("employeeOccupationSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.employeeOccupationSchemePromoter"))
    }

    "fail if we have only have the farmHousesScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(farmHousesScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("farmHousesSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.farmHousesSchemePromoter"))
    }

    "fail if we have only have the socialHousingScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(socialHousingScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("socialHousingSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.socialHousingSchemePromoter"))
    }

    "fail if we have only have the socialHousingScheme and year is 2021" in {
      val emptyTaxAvoidance = TaxAvoidance(socialHousingScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey2021)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("socialHousingSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.providerSocialOrHousingSchemePromoter"))
    }

    "fail if we have only have the equityReleaseScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(equityReleaseScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("equityReleaseSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.equityReleaseSchemePromoter"))
    }

    "fail if we have only have the promoter" in {
      val emptyTaxAvoidance = TaxAvoidance(rentalBusinessSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("rentalBusinessScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.rentalBusinessScheme"))
    }

    "fail if we have only have the openToPublicSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(openToPublicSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("openToPublicScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.openToPublicScheme"))
    }

    "fail if we have only have the propertyDeveloperSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(propertyDeveloperSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("propertyDeveloperScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.propertyDeveloperScheme"))
    }

    "fail if we have only have the propertyTradingSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(propertyTradingSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("propertyTradingScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.propertyTradingScheme"))
    }

    "fail if we have only have the lendingSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(lendingSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("lendingScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.lendingScheme"))
    }

    "fail if we have only have the employeeOccupationSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(employeeOccupationSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("employeeOccupationScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.employeeOccupationScheme"))
    }

    "fail if we have only have the farmHousesSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(farmHousesSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("farmHousesScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.farmHousesScheme"))
    }

    "fail if we have only have the socialHousingSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(socialHousingSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("socialHousingScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.socialHousingScheme"))
    }

    "fail if we have only have the socialHousingSchemePromoter and year is 2021" in {
      val emptyTaxAvoidance = TaxAvoidance(socialHousingSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey2021)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("socialHousingScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.providerSocialOrHousingScheme"))
    }

    "fail if we have only have the equityReleaseSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(equityReleaseSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("equityReleaseScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.empty.equityReleaseScheme"))
    }

    "fail if the avoidance scheme is not 8 digits" in {
      val errSize: Int = 4
      val taxAvoidance: TaxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("123"),
        rentalBusinessSchemePromoter = Some("123"),
        openToPublicScheme = Some("123"),
        openToPublicSchemePromoter = Some("123")
      )
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(taxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (errSize)
      result.error("rentalBusinessScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.wrong-length.rentalBusinessScheme"))
      result.error("openToPublicScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.wrong-length.openToPublicScheme"))
      result.error("rentalBusinessSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.wrong-length.rentalBusinessSchemePromoter"))
      result.error("openToPublicSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.wrong-length.openToPublicSchemePromoter"))
    }

    "fail if the avoidance scheme is 8 characters" in {
      val errSize: Int = 4
      val taxAvoidance: TaxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("1234567a"),
        rentalBusinessSchemePromoter = Some("1234567a"),
        openToPublicScheme = Some("1234567a"),
        openToPublicSchemePromoter = Some("1234567a")
      )
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(taxAvoidance), periodKey)
      result.hasErrors must be(true)
      result.errors.size must be (errSize)
      result.error("rentalBusinessScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.numeric-error.rentalBusinessScheme"))
      result.error("openToPublicScheme").map(_.message) must be (Some("ated.avoidance-scheme-error.general.numeric-error.openToPublicScheme"))
      result.error("rentalBusinessSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.numeric-error.rentalBusinessSchemePromoter"))
      result.error("openToPublicSchemePromoter").map(_.message) must be (Some("ated.avoidance-scheme-error.general.numeric-error.openToPublicSchemePromoter"))
    }
  }

  "avoidanceSchemeConstraint" must {
    "throw validation" when {
      "avoidance scheme is not available" in {
        val form: Form[IsTaxAvoidance] = ReliefForms.isTaxAvoidanceForm.bind(Json.obj(), maxChars)
        form.hasErrors mustBe true
        form.errors must contain (FormError("", Seq("ated.claim-relief.avoidance-scheme.selected"),Seq("isAvoidanceScheme")))
      }
    }

    "not throw validation" when {
      "avoidance scheme is available" in {
        val form: Form[IsTaxAvoidance] = ReliefForms.isTaxAvoidanceForm.bind(Json.obj("isAvoidanceScheme" -> false), maxChars)
        form.hasErrors mustBe false
      }
    }
  }

  "validateForm" must {
    val field = "rentalBusiness"
    val fieldStartDate = field + "Date"

    "throw validation error" when {
      "reliefSelected is true and start date is empty" in {

        val formWithErrors: Form[Reliefs] = validateForm(reliefsForm.bind(Json.obj("periodKey" -> periodKey, field -> true), maxChars))
        val expectedError = s"ated.choose-reliefs.error.date.mandatory.$fieldStartDate"
        formWithErrors.errors mustBe Seq(FormError(fieldStartDate, expectedError))
      }

      "reliefSelected is true and period is too early" in {

        val formWithErrors: Form[Reliefs] = validateForm(reliefsForm.bind(Json.obj("periodKey" -> periodKey, field -> true, fieldStartDate -> Map("day" -> "1", "month" -> "4", "year" -> (periodKey - 1).toString)), maxChars))
        val expectedError = s"ated.choose-reliefs.error.date.chargePeriod.$fieldStartDate"
        formWithErrors.errors mustBe Seq(FormError(fieldStartDate, expectedError))
      }

      "reliefSelected is true and period is too late" in {
        val formWithErrors: Form[Reliefs] = validateForm(reliefsForm.bind(Json.obj("periodKey" -> periodKey, field -> true, fieldStartDate -> Map("day" -> "1", "month" -> "4", "year" -> (periodKey + 1).toString)), maxChars))
        val expectedError = s"ated.choose-reliefs.error.date.chargePeriod.$fieldStartDate"
        formWithErrors.errors mustBe Seq(FormError(fieldStartDate, expectedError))
      }

      "nothing is selected" in {
        val formWithErrors: Form[Reliefs] = validateForm(reliefsForm.bind(Json.obj("periodKey" -> periodKey), maxChars))
        val expectedError = "ated.choose-reliefs.error"
        formWithErrors.errors mustBe Seq(FormError("", expectedError, Seq(field)))
      }
    }

    "reliefSelected is true and start date is within the taxable period" in {
      val formWithErrors: Form[Reliefs] = validateForm(reliefsForm.bind(Json.obj("periodKey" -> periodKey, field -> true, fieldStartDate -> Map("day" -> "31", "month" -> "3", "year" -> (periodKey).toString)), maxChars))
      val expectedError = s"ated.choose-reliefs.error.date.chargePeriod.$fieldStartDate"
      formWithErrors.errors mustBe Seq(FormError(fieldStartDate, expectedError))
    }

    " reliefSelected is true but letters are passed into the day field" in {
      val formWithErrors: Form[Reliefs] = validateForm(reliefsForm.bind(Json.obj("periodKey" -> periodKey, field -> true, fieldStartDate -> Map("day" -> "aa", "month" -> "4", "year" -> (periodKey + 1).toString)), maxChars))
      val expectedError = s"ated.choose-reliefs.error.date.mandatory.$fieldStartDate"
      formWithErrors.errors mustBe Seq(FormError(fieldStartDate, expectedError))
    }

    " reliefSelected is true but letters are passed into the month field" in {
      val formWithErrors: Form[Reliefs] = validateForm(reliefsForm.bind(Json.obj("periodKey" -> periodKey, field -> true, fieldStartDate -> Map("day" -> "31", "month" -> "aa", "year" -> (periodKey + 1).toString)), maxChars))
      val expectedError = s"ated.choose-reliefs.error.date.mandatory.$fieldStartDate"
      formWithErrors.errors mustBe Seq(FormError(fieldStartDate, expectedError))
    }

    " reliefSelected is true but letters are passed into the year field" in {
      val formWithErrors: Form[Reliefs] = validateForm(reliefsForm.bind(Json.obj("periodKey" -> periodKey, field -> true, fieldStartDate -> Map("day" -> "31", "month" -> "4", "year" -> "aaaa")), maxChars))
      val expectedError = s"ated.choose-reliefs.error.date.mandatory.$fieldStartDate"
      formWithErrors.errors mustBe Seq(FormError(fieldStartDate, expectedError))
    }

  }

  "reliefSelectedConstraint" must {
    "throw validation error" when {
      "there is no relief option selected" in {
        val form: Form[Reliefs] = ReliefForms.reliefsForm.bind(Json.obj("periodKey" -> periodKey), maxChars)
        form.hasErrors mustBe true
        form.errors must contain
                     List(FormError("", List("ated.choose-reliefs.error"),List("rentalBusiness")))
      }
    }

    "not throw validation error" when {
      "there is a relief option selected" in {
        val form: Form[Reliefs] = ReliefForms.reliefsForm.bind(Json.obj("periodKey" -> periodKey,
          "rentalBusiness" -> true, "rentalBusinessDate" ->  Map("day" -> "1", "month" -> "7", "year" -> periodKey.toString)), maxChars)
        form.hasErrors mustBe false
      }
    }
  }
}
