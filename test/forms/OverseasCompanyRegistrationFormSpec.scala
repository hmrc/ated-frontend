/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class OverseasCompanyRegistrationFormSpec extends PlaySpec with OneServerPerSuite {

  val companyFormData: Map[String, String] = Map(
    "businessUniqueId" -> "Unique&-Id",
    "issuingInstitution" -> "institute&'/",
    "countryCode" -> "EE"
  )

  val invalidCompanyFormData: Map[String, String] = Map(
    "businessUniqueId" -> "Unique&-Id***",
    "issuingInstitution" -> "institute&'/$$",
    "countryCode" -> "GB$"
  )

  val maxLengthCompanyFormData: Map[String, String] = Map(
    "businessUniqueId" -> "ab"*61,
    "issuingInstitution" -> "abc"*42,
    "countryCode" -> "GB"*2
  )

  "OverseasCompanyRegistrationForm" must {
    "render overseasCompanyRegistrationForm successfully on entering valid input data" in {
      OverseasCompanyRegistrationForm.overseasCompanyRegistrationForm.bind(companyFormData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success => {
          success.businessUniqueId must be(Some("Unique&-Id"))
          success.issuingInstitution must be(Some("institute&'/"))
          success.countryCode must be(Some("EE"))
        }
      )
    }

    "throw overseasCompanyRegistrationForm error on entering invalid input data" in {
      OverseasCompanyRegistrationForm.overseasCompanyRegistrationForm.bind(invalidCompanyFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("The overseas company registration number must only include letters a to z, numbers 0 to 9, ampersands (&), apostrophes (‘) and hyphens (-)")
          formWithErrors.errors(1).message must be("Issuing institution must only include letters a to z, numbers 0 to 9, ampersands (&), apostrophes (‘), forward slashes (/) and hyphens (-)")
          formWithErrors.errors(2).message must be("Enter a country outside of United Kingdom and Great Britain")
          formWithErrors.errors.length must be(3)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "throw overseasCompanyRegistrationForm error on entering input data which exceeds max length" in {
      OverseasCompanyRegistrationForm.overseasCompanyRegistrationForm.bind(maxLengthCompanyFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("The overseas company registration number cannot be more than 60 characters")
          formWithErrors.errors(1).message must be("The institution that issued the overseas company registration number cannot be more than 40 characters")
          formWithErrors.errors(2).message must be("Enter a country outside of United Kingdom and Great Britain")
          formWithErrors.errors.length must be(3)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }
}
