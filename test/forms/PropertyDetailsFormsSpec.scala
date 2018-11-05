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

class PropertyDetailsFormsSpec extends PlaySpec with OneServerPerSuite {

  val propertyDetailsAddressFormData: Map[String, String] = Map(
    "line_1" -> "address&Id",
    "line_2" -> "institute&'",
    "line_3" -> "some street & Some road",
    "line_4" -> "&&&",
    "postcode" -> "AA1 1AA"
  )

  val invalidPropertyDetailsAddressFormData: Map[String, String] = Map(
    "line_1" -> "address&-Id***",
    "line_2" -> "institute&'/$$",
    "line_3" -> "institute&'/$$",
    "line_4" -> "institute&'/$$",
    "postcode" -> "AA1 ***$"
  )

  val maxLengthPropertyDetailsAddressFormData: Map[String, String] = Map(
    "line_1" -> "ab"*30,
    "line_2" -> "abc"*30,
    "line_3" -> "GB"*20,
    "line_4" -> "GB"*20,
    "postcode" -> "G"*11

  )

  "propertyDetailsAddressForm" must {
    "render propertyDetailsAddressForm successfully on entering valid input data" in {
      PropertyDetailsForms.propertyDetailsAddressForm.bind(propertyDetailsAddressFormData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success => {
          success.line_1 must be("address&Id")
          success.line_2 must be("institute&'")
          success.postcode must be(Some("AA1 1AA"))
        }
      )
    }

    "throw error on entering invalid input data" in {
      PropertyDetailsForms.propertyDetailsAddressForm.bind(invalidPropertyDetailsAddressFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("You must enter a valid address line 1")
          formWithErrors.errors(1).message must be("You must enter a valid address line 2")
          formWithErrors.errors(2).message must be("You must enter a valid address line 3")
          formWithErrors.errors(3).message must be("You must enter a valid address line 4")
          formWithErrors.errors(4).message must be("You must enter a valid postcode")
          formWithErrors.errors.length must be(5)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "throw error on entering input data which exceeds max length" in {
      PropertyDetailsForms.propertyDetailsAddressForm.bind(maxLengthPropertyDetailsAddressFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("Address line 1 cannot be more than 35 characters")
          formWithErrors.errors(1).message must be("Address line 2 cannot be more than 35 characters")
          formWithErrors.errors(2).message must be("Address line 3 cannot be more than 35 characters")
          formWithErrors.errors(3).message must be("Address line 4 cannot be more than 35 characters")
          formWithErrors.errors(4).message must be("The postcode cannot be more than 10 characters")
          formWithErrors.errors.length must be(5)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }

  "propertyDetailsTitleForm" must {
    "pass through the validation when enetered valid data" in {
      val validData = Map("titleNumber" -> "125341524")
      PropertyDetailsForms.propertyDetailsTitleForm.bind(validData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success => {
          success.titleNumber must be("125341524")
        }
      )
    }

     "throw error on entering input data which exceeds max length" in {
       val validData = Map("titleNumber" -> "125341524"*10)
       PropertyDetailsForms.propertyDetailsTitleForm.bind(validData).fold(
         formWithErrors => {
           formWithErrors.errors.head.message must be("Property title number cannot be more than 40 characters")
           formWithErrors.errors.length must be(1)
         },
         success => {

         }
       )
     }

    "throw error on entering invalid input data" in {
      val validData = Map("titleNumber" -> "125341524&£',.")
      PropertyDetailsForms.propertyDetailsTitleForm.bind(validData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("You cannot enter special characters. For example £, or @.")
          formWithErrors.errors.length must be(1)
        },
        success => {
          success.titleNumber must be("125341524")
        }
      )
    }
  }



}
