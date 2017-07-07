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

package forms

import forms.ReliefForms.taxAvoidanceForm
import models._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class ReliefFormsSpec extends PlaySpec with OneServerPerSuite {

  val noPromoterErrorMessage = "You must enter a reference number"
  val noSchemeErrorMessage = "You must enter a reference number"
  "validateTaxAvoidance" must {
    "fail if we have no data" in {

      val emptyTaxAvoidance = TaxAvoidance()
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.errors.head.key must be ("empty")
      result.errors.head.message must be ("")
    }

    "fail with only 1 error if we have a rental scheme and empty public Scheme" in {
      val emptyTaxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("12345678"), openToPublicScheme = Some(""))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("rentalBusinessSchemePromoter").map(_.message) must be (Some(noPromoterErrorMessage))
    }

    "fail if we have only have the rental scheme" in {
      val emptyTaxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("rentalBusinessSchemePromoter").map(_.message) must be (Some(noPromoterErrorMessage))
    }

    "fail if we have only have the openToPublicScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(openToPublicScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("openToPublicSchemePromoter").map(_.message) must be (Some(noPromoterErrorMessage))
    }

    "fail if we have only have the propertyDeveloperScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(propertyDeveloperScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("propertyDeveloperSchemePromoter").map(_.message) must be (Some(noPromoterErrorMessage))
    }

    "fail if we have only have the propertyTradingScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(propertyTradingScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("propertyTradingSchemePromoter").map(_.message) must be (Some(noPromoterErrorMessage))
    }

    "fail if we have only have the lendingScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(lendingScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("lendingSchemePromoter").map(_.message) must be (Some(noPromoterErrorMessage))
    }

    "fail if we have only have the employeeOccupationScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(employeeOccupationScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("employeeOccupationSchemePromoter").map(_.message) must be (Some(noPromoterErrorMessage))
    }

    "fail if we have only have the farmHousesScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(farmHousesScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("farmHousesSchemePromoter").map(_.message) must be (Some(noPromoterErrorMessage))
    }

    "fail if we have only have the socialHousingScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(socialHousingScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("socialHousingSchemePromoter").map(_.message) must be (Some(noPromoterErrorMessage))
    }

    "fail if we have only have the equityReleaseScheme" in {
      val emptyTaxAvoidance = TaxAvoidance(equityReleaseScheme = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("equityReleaseSchemePromoter").map(_.message) must be (Some(noPromoterErrorMessage))
    }

    "fail if we have only have the promoter" in {
      val emptyTaxAvoidance = TaxAvoidance(rentalBusinessSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("rentalBusinessScheme").map(_.message) must be (Some(noSchemeErrorMessage))
    }

    "fail if we have only have the openToPublicSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(openToPublicSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("openToPublicScheme").map(_.message) must be (Some(noSchemeErrorMessage))
    }

    "fail if we have only have the propertyDeveloperSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(propertyDeveloperSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("propertyDeveloperScheme").map(_.message) must be (Some(noSchemeErrorMessage))
    }

    "fail if we have only have the propertyTradingSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(propertyTradingSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("propertyTradingScheme").map(_.message) must be (Some(noSchemeErrorMessage))
    }

    "fail if we have only have the lendingSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(lendingSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("lendingScheme").map(_.message) must be (Some(noSchemeErrorMessage))
    }

    "fail if we have only have the employeeOccupationSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(employeeOccupationSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("employeeOccupationScheme").map(_.message) must be (Some(noSchemeErrorMessage))
    }

    "fail if we have only have the farmHousesSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(farmHousesSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("farmHousesScheme").map(_.message) must be (Some(noSchemeErrorMessage))
    }

    "fail if we have only have the socialHousingSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(socialHousingSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("socialHousingScheme").map(_.message) must be (Some(noSchemeErrorMessage))
    }

    "fail if we have only have the equityReleaseSchemePromoter" in {
      val emptyTaxAvoidance = TaxAvoidance(equityReleaseSchemePromoter = Some("12345678"))
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(emptyTaxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (1)
      result.error("equityReleaseScheme").map(_.message) must be (Some(noSchemeErrorMessage))
    }

    "fail if the avoidance scheme is not 8 digits" in {
      val taxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("123"),
        rentalBusinessSchemePromoter = Some("123"),
        openToPublicScheme = Some("123"),
        openToPublicSchemePromoter = Some("123")
      )
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(taxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (4)
      result.error("rentalBusinessScheme").map(_.message) must be (Some("The avoidance scheme number must be 8 digits."))
      result.error("openToPublicScheme").map(_.message) must be (Some("The avoidance scheme number must be 8 digits."))
      result.error("rentalBusinessSchemePromoter").map(_.message) must be (Some("The promoter reference number must be 8 digits."))
      result.error("openToPublicSchemePromoter").map(_.message) must be (Some("The promoter reference number must be 8 digits."))
    }

    "fail if the avoidance scheme is 8 characters" in {
      val taxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("1234567a"),
        rentalBusinessSchemePromoter = Some("1234567a"),
        openToPublicScheme = Some("1234567a"),
        openToPublicSchemePromoter = Some("1234567a")
      )
      val result = ReliefForms.validateTaxAvoidance(taxAvoidanceForm.fill(taxAvoidance))
      result.hasErrors must be(true)
      result.errors.size must be (4)
      result.error("rentalBusinessScheme").map(_.message) must be (Some("The avoidance scheme number can only contain numbers"))
      result.error("openToPublicScheme").map(_.message) must be (Some("The avoidance scheme number can only contain numbers"))
      result.error("rentalBusinessSchemePromoter").map(_.message) must be (Some("The promoter reference number can only contain numbers"))
      result.error("openToPublicSchemePromoter").map(_.message) must be (Some("The promoter reference number can only contain numbers"))
    }

  }
}
