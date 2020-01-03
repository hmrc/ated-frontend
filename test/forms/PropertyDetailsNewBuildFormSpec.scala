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

package forms

import forms.PropertyDetailsForms.propertyDetailsNewBuildForm
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class PropertyDetailsNewBuildFormSpec extends PlaySpec with MustMatchers with GuiceOneServerPerSuite {

  "propertyDetailsNewBuildForm" must {
    "throw error" when {
      "form is empty" in {
        val form = propertyDetailsNewBuildForm.bind(Map.empty[String, String])
        form.fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "ated.property-details-value.isNewBuild.error.non-selected"
          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "Option 'yes' is selected and newBuild date,localAuthReg date and new build value are empty" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isNewBuild" -> "true",
          "newBuildDate-day" -> "",
          "newBuildDate.month" -> "",
          "newBuildDate.year" -> "",
          "newBuildValue" -> "",
          "localAuthRegDate-day" -> "",
          "localAuthRegDate.month" -> "",
          "localAuthRegDate.year" -> ""
        )
        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  3
            hasErrors.errors.head.message mustBe "ated.property-details-value.newBuildDate.error.empty"
            hasErrors.errors(1).message mustBe "ated.property-details-value.localAuthRegDate.error.empty"
            hasErrors.errors.last.message mustBe "ated.property-details-value.newBuildValue.error.empty"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and newBuild date and localAuthReg date are invalid" in {
        val periodKey = 2018
          val input: Map[String, String] =  Map("isNewBuild" -> "true",
          "newBuildDate.day" -> "13",
          "newBuildDate.month" -> "10",
          "newBuildDate.year" -> "2030",
          "newBuildValue" -> "150000000",
          "localAuthRegDate.day" -> "12",
          "localAuthRegDate.month" -> "10",
          "localAuthRegDate.year" -> "2030"
        )

        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  2
            hasErrors.errors.head.message mustBe "ated.property-details-value.newBuildDate.error.too-late"
            hasErrors.errors.last.message mustBe "ated.property-details-value.localAuthRegDate.error.too-late"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and newBuild date and localAuthReg date is too early" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isNewBuild" -> "true",
          "newBuildDate.day" -> "13",
          "newBuildDate.month" -> "03",
          "newBuildDate.year" -> "2017",
          "newBuildValue" -> "150000000",
          "localAuthRegDate.day" -> "12",
          "localAuthRegDate.month" -> "03",
          "localAuthRegDate.year" -> "2017"
        )

        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  2
            hasErrors.errors.head.message mustBe "ated.property-details-value.newBuildDate.error.too-early"
            hasErrors.errors.last.message mustBe "ated.property-details-value.localAuthRegDate.error.too-early"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and newBuild date and localAuthReg date are not filled correctly" in {
        val periodKey = 2018
         val input: Map[String, String] =  Map("isNewBuild" -> "true",
          "newBuildDate.day" -> "13",
          "newBuildDate.month" -> "",
          "newBuildDate.year" -> "",
          "newBuildValue" -> "150000000",
          "localAuthRegDate.day" -> "12",
          "localAuthRegDate.month" -> "",
          "localAuthRegDate.year" -> ""
        )

        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  2
            hasErrors.errors.head.message mustBe "error.invalid.date.format"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and newBuild value is invalid" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isNewBuild" -> "true",
          "newBuildDate.day" -> "13",
          "newBuildDate.month" -> "10",
          "newBuildDate.year" -> "2010",
          "newBuildValue" -> "ahgfhagsfhafshg",
          "localAuthRegDate.day" -> "12",
          "localAuthRegDate.month" -> "10",
          "localAuthRegDate.year" -> "2010"
        )
        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.incorrect-format"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }


      "Option 'yes' is selected and newBuild value is too low" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isNewBuild" -> "true",
          "newBuildDate.day" -> "13",
          "newBuildDate.month" -> "10",
          "newBuildDate.year" -> "2018",
          "newBuildValue" -> "500000",
          "localAuthRegDate.day" -> "12",
          "localAuthRegDate.month" -> "10",
          "localAuthRegDate.year" -> "2018"
        )
        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.newBuildValue.error.too-low"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }


      "Option 'yes' is selected and newBuild value is too high" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isNewBuild" -> "true",
          "newBuildDate.day" -> "13",
          "newBuildDate.month" -> "10",
          "newBuildDate.year" -> "2018",
          "newBuildValue" -> "10000000000000",
          "localAuthRegDate.day" -> "12",
          "localAuthRegDate.month" -> "10",
          "localAuthRegDate.year" -> "2018"
        )
        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.newBuildValue.error.too-high"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'no' is selected and acquired date, value of the property is empty" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isNewBuild" -> "false",
          "notNewBuildDate-day" -> "",
          "notNewBuildDate.month" -> "",
          "notNewBuildDate.year" -> "",
          "notNewBuildValue" -> ""
        )
        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  2
            hasErrors.errors.head.message mustBe "ated.property-details-value.notNewBuildDate.error.empty"
            hasErrors.errors.last.message mustBe "ated.property-details-value.notNewBuildValue.error.empty"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'no' is selected and acquired date is invalid" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isNewBuild" -> "false",
          "notNewBuildDate.day" -> "13",
          "notNewBuildDate.month" -> "10",
          "notNewBuildDate.year" -> "2030",
          "notNewBuildValue" -> "150000000"
        )

        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.notNewBuildDate.error.too-late"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'no' is selected and value of the property is invalid" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isNewBuild" -> "false",
          "notNewBuildDate.day" -> "13",
          "notNewBuildDate.month" -> "10",
          "notNewBuildDate.year" -> "2018",
          "notNewBuildValue" -> "asgdjhagsdjgasjh"
        )

        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.incorrect-format"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'no' is selected and value of the property is too low" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isNewBuild" -> "false",
          "notNewBuildDate.day" -> "13",
          "notNewBuildDate.month" -> "10",
          "notNewBuildDate.year" -> "2018",
          "notNewBuildValue" -> "500000"
        )

        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.notNewBuildValue.error.too-low"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'no' is selected and value of the property exceeds max length" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isNewBuild" -> "false",
          "notNewBuildDate.day" -> "13",
          "notNewBuildDate.month" -> "10",
          "notNewBuildDate.year" -> "2018",
          "notNewBuildValue" -> "10000000000000"
        )

        PropertyDetailsForms.validatePropertyDetailsNewBuild(periodKey,  propertyDetailsNewBuildForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.notNewBuildValue.error.too-high"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }
    }
  }
}
