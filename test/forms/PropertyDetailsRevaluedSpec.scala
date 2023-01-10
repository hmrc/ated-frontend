/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.PropertyDetailsForms.propertyDetailsRevaluedForm
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class PropertyDetailsRevaluedSpec extends PlaySpec with GuiceOneServerPerSuite {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())

  "PropertyDetailsRevaluedForm" must {
    "throw error" when {
      "form is empty" in {
        val form = propertyDetailsRevaluedForm.bind(Map.empty[String, String])
        form.fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "ated.property-details-value.isPropertyRevalued.error.non-selected"
          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "Option 'yes' is selected and change date, valuation value and revalued date are empty" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "",
          "partAcqDispDate.month" -> "",
          "partAcqDispDate.year" -> "",
          "revaluedValue" -> "",
          "revaluedDate.day" -> "",
          "revaluedDate.month" -> "",
          "revaluedDate.year" -> ""
        )
        PropertyDetailsForms.validatePropertyDetailsRevalued(periodKey,  propertyDetailsRevaluedForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  3
            hasErrors.errors.head.message mustBe "ated.property-details-value.partAcqDispDate.error.empty"
            hasErrors.errors(1).message mustBe "ated.property-details-value.revaluedValue.error.empty"
            hasErrors.errors.last.message mustBe "ated.property-details-value.revaluedDate.error.empty"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and change date, revalued date are invalid" in {
        val periodKey = 2018
          val input: Map[String, String] =  Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "13",
          "partAcqDispDate.month" -> "10",
          "partAcqDispDate.year" -> "2030",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "12",
          "revaluedDate.month" -> "10",
          "revaluedDate.year" -> "2030"
        )

        PropertyDetailsForms.validatePropertyDetailsRevalued(periodKey,  propertyDetailsRevaluedForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  2
            hasErrors.errors.head.message mustBe "ated.property-details-value.partAcqDispDate.error.in-future"
            hasErrors.errors.last.message mustBe "ated.property-details-value.revaluedDate.error.in-future"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and change date and revalued date are not filled correctly" in {
        val periodKey = 2018
         val input: Map[String, String] =  Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "13",
          "partAcqDispDate.month" -> "",
          "partAcqDispDate.year" -> "",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "12",
          "revaluedDate.month" -> "",
          "revaluedDate.year" -> ""
        )

        PropertyDetailsForms.validatePropertyDetailsRevalued(periodKey,  propertyDetailsRevaluedForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  2
            hasErrors.errors.head.message mustBe "error.invalid.date.format"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and valuation value has invalid data" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "13",
          "partAcqDispDate.month" -> "10",
          "partAcqDispDate.year" -> "2010",
          "revaluedValue" -> "ahgfhagsfhafshg",
          "revaluedDate.day" -> "12",
          "revaluedDate.month" -> "10",
          "revaluedDate.year" -> "2010"
        )
        PropertyDetailsForms.validatePropertyDetailsRevalued(periodKey,  propertyDetailsRevaluedForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.incorrect-format"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and valuation value is too low" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "13",
          "partAcqDispDate.month" -> "10",
          "partAcqDispDate.year" -> "2010",
          "revaluedValue" -> "499999",
          "revaluedDate.day" -> "12",
          "revaluedDate.month" -> "10",
          "revaluedDate.year" -> "2010"
        )
        PropertyDetailsForms.validatePropertyDetailsRevalued(periodKey,  propertyDetailsRevaluedForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.revaluedValue.error.too-low"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and valuation value is too high" in {
        val periodKey = 2018
        val input: Map[String, String] =  Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "13",
          "partAcqDispDate.month" -> "10",
          "partAcqDispDate.year" -> "2010",
          "revaluedValue" -> "10000000000000",
          "revaluedDate.day" -> "12",
          "revaluedDate.month" -> "10",
          "revaluedDate.year" -> "2010"
        )
        PropertyDetailsForms.validatePropertyDetailsRevalued(periodKey,  propertyDetailsRevaluedForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.revaluedValue.error.too-high"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }
    }
  }
}
