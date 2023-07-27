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
import forms.mappings.DateTupleCustomError
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class PropertyDetailsRevaluedSpec extends PlaySpec with GuiceOneServerPerSuite {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())

  val dateFields = Seq(
    ("partAcqDispDate", "The date when you made the £40,000 or more change"),
    ("revaluedDate", "Revaluation date")
  )

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
            hasErrors.errors.length mustBe  2
            hasErrors.errors.head.message mustBe "ated.property-details-value.partAcqDispDate.error.empty"
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
            hasErrors.errors.head.message mustBe "ated.property-details-value.partAcqDispDate.error.empty"
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
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey,  propertyDetailsRevaluedForm.bind(input), dateFields).fold(
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
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey,  propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.revaluedValue.error.too-high"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and valuation value is empty" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "13",
          "partAcqDispDate.month" -> "10",
          "partAcqDispDate.year" -> "2010",
          "revaluedValue" -> "",
          "revaluedDate.day" -> "12",
          "revaluedDate.month" -> "10",
          "revaluedDate.year" -> "2010"
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "ated.property-details-value.revaluedValue.error.empty"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and revaluedDate.day and partAcqDispDate.day are empty" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "",
          "partAcqDispDate.month" -> "10",
          "partAcqDispDate.year" -> "2010",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "",
          "revaluedDate.month" -> "10",
          "revaluedDate.year" -> "2010"
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.date.day.missing"
            hasErrors.errors.last.message mustBe "ated.error.date.day.missing"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and revaluedDate.month and partAcqDispDate.month are empty" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "12",
          "partAcqDispDate.month" -> "",
          "partAcqDispDate.year" -> "2010",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "11",
          "revaluedDate.month" -> "",
          "revaluedDate.year" -> "2010"
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.date.month.missing"
            hasErrors.errors.last.message mustBe "ated.error.date.month.missing"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and revaluedDate.year and partAcqDispDate.year are empty" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "11",
          "partAcqDispDate.month" -> "10",
          "partAcqDispDate.year" -> "",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "12",
          "revaluedDate.month" -> "10",
          "revaluedDate.year" -> ""
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.date.year.missing"
            hasErrors.errors.last.message mustBe "ated.error.date.year.missing"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and day and month for revaluedDate and partAcqDispDate are empty" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "",
          "partAcqDispDate.month" -> "",
          "partAcqDispDate.year" -> "2015",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "",
          "revaluedDate.month" -> "",
          "revaluedDate.year" -> "2016"
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.date.daymonth.missing"
            hasErrors.errors.last.message mustBe "ated.error.date.daymonth.missing"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and day and year for revaluedDate and partAcqDispDate are empty" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "",
          "partAcqDispDate.month" -> "10",
          "partAcqDispDate.year" -> "",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "",
          "revaluedDate.month" -> "10",
          "revaluedDate.year" -> ""
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.date.dayyear.missing"
            hasErrors.errors.last.message mustBe "ated.error.date.dayyear.missing"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and month and year for revaluedDate and partAcqDispDate are empty" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "11",
          "partAcqDispDate.month" -> "",
          "partAcqDispDate.year" -> "",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "12",
          "revaluedDate.month" -> "",
          "revaluedDate.year" -> ""
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.date.monthyear.missing"
            hasErrors.errors.last.message mustBe "ated.error.date.monthyear.missing"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and value input for year is not of length 4" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "11",
          "partAcqDispDate.month" -> "2",
          "partAcqDispDate.year" -> "123",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "12",
          "revaluedDate.month" -> "4",
          "revaluedDate.year" -> "34"
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.date.year.length"
            hasErrors.errors.last.message mustBe "ated.error.date.year.length"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and invalid day & month combination for partAcqDispDate and revaluedDate" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "30",
          "partAcqDispDate.month" -> "2",
          "partAcqDispDate.year" -> "2015",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "31",
          "revaluedDate.month" -> "4",
          "revaluedDate.year" -> "2016"
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.date.invalid.day.month"
            hasErrors.errors.last.message mustBe "ated.error.date.invalid.day.month"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and invalid day values for partAcqDispDate and revaluedDate" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "43",
          "partAcqDispDate.month" -> "4",
          "partAcqDispDate.year" -> "2015",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "34",
          "revaluedDate.month" -> "5",
          "revaluedDate.year" -> "2016"
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.day.invalid"
            hasErrors.errors.last.message mustBe "ated.error.day.invalid"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and invalid month values for partAcqDispDate and revaluedDate" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "3",
          "partAcqDispDate.month" -> "29",
          "partAcqDispDate.year" -> "2015",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "4",
          "revaluedDate.month" -> "189",
          "revaluedDate.year" -> "2016"
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.month.invalid"
            hasErrors.errors.last.message mustBe "ated.error.month.invalid"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and empty values for partAcqDispDate and revaluedDate" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "",
          "partAcqDispDate.month" -> "",
          "partAcqDispDate.year" -> "",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "",
          "revaluedDate.month" -> "",
          "revaluedDate.year" -> ""
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.date.empty"
            hasErrors.errors.last.message mustBe "ated.error.date.empty"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and invalid values for partAcqDispDate and revaluedDate" in {
        val periodKey = 2018
        val input: Map[String, String] = Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "3",
          "partAcqDispDate.month" -> "6",
          "partAcqDispDate.year" -> "fgbgfb",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "4",
          "revaluedDate.month" -> "6",
          "revaluedDate.year" -> "fgbfb"
        )
        PropertyDetailsForms.validatePropertyDetailsRevaluedForm(periodKey, propertyDetailsRevaluedForm.bind(input), dateFields).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.error.date.invalid"
            hasErrors.errors.last.message mustBe "ated.error.date.invalid"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and past date values for partAcqDispDate and revaluedDate" in {
        DateTupleCustomError.validateDateFields(Some("2"), Some("3"), Some("2011"), dateFields, Some(LocalDate.now())).map(
          x => x.message mustBe "ated.error.date.past"
        )
      }
    }
  }
}
