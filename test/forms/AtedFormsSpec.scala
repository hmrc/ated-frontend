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

class AtedFormsSpec extends PlaySpec with OneServerPerSuite {

  "validatePostCodeFormat" must {
    "return true for a valid postcode" in {
      AtedForms.validatePostCodeFormat(Some("AA1 1AA")) must be(true)
    }

    "return false for an invalid postcode" in {
      AtedForms.validatePostCodeFormat(Some("ZZ1")) must be(false)
    }
  }

  "validateAddressLine" must {
    "return true for a blank address line" in {
      AtedForms.validateAddressLine(Some(" ")) must be(true)
    }

    "return true for a valid address line" in {
      AtedForms.validateAddressLine(Some("1 addressLine1")) must be(true)
    }

    "return true for a valid address line with special characters" in {
      AtedForms.validateAddressLine(Some("12-3 Fake Street, & Road's.")) must be(true)
    }

    "return false for an invalid address line with special characters" in {
      AtedForms.validateAddressLine(Some("<select all addresses>")) must be(false)
    }
  }

   "editContactDetailsForm" must {

     val invalidFormData: Map[String, String] = Map(
       "firstName" -> "firstName****^$£",
       "lastName" -> "lastName*******&&&",
       "phoneNumber" -> "7879797979999**^"
     )

     val maxLengthFormData: Map[String, String] = Map(
       "firstName" -> "firstName"*10,
       "lastName" -> "lastName"*10,
       "phoneNumber" -> "787979797"*10
     )

     "fail form validation on invalid form data" in {
       AtedForms.editContactDetailsForm.bind(invalidFormData).fold(
         formWithErrors => {
           formWithErrors.errors.head.message must be("ated.contact-details-first-name.invalid")
           formWithErrors.errors(1).message must be("ated.contact-details-first-name.invalid")
           formWithErrors.errors(2).message must be("Telephone number is not valid")
           formWithErrors.errors.length must be(3)
         },
         success => {
         }
       )
     }

      "fail form validation when form data exceeds max length" in {
        AtedForms.editContactDetailsForm.bind(maxLengthFormData).fold(
          formWithErrors => {
            formWithErrors.errors.head.message must be("First name cannot be more than 35 characters")
            formWithErrors.errors(1).message must be("Last name cannot be more than 35 characters")
            formWithErrors.errors(2).message must be("Telephone number must not be more than 24 characters")
            formWithErrors.errors.length must be(3)
          },
          success => {
          }
        )
      }
   }

}
