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

import forms.PropertyDetailsForms.{propertyDetailsRevaluedForm, propertyDetailsTaxAvoidanceForm, validatePropertyDetailsTaxAvoidance}
import org.joda.time.LocalDate
import org.scalatest.Matchers
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

class PropertyDetailsRevaluedSpec extends UnitSpec with Matchers with OneAppPerSuite {

  implicit lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages = messagesApi.preferred(FakeRequest())

  "PropertyDetailsRevaluedForm" must {
    "throw error" when {
      "form is empty" in {
        val form = propertyDetailsRevaluedForm.bind(Map.empty[String, String])
        form.fold(
          hasErrors => {
            hasErrors.errors.length shouldBe 1
            hasErrors.errors.head.message shouldBe Messages("ated.property-details-value.isPropertyRevalued.error.non-selected")
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
            hasErrors.errors.length shouldBe  3
            hasErrors.errors.head.message shouldBe Messages("ated.property-details-value.partAcqDispDate.error.empty")
            hasErrors.errors(1).message shouldBe Messages("q2ated.property-details-value.ownedBefore2012Value.error.empty")
            hasErrors.errors.last.message shouldBe Messages("ated.property-details-value.revaluedDate.error.empty")
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and change date, revalued date have invalid data" in {
        val periodKey = 2018
        val currentDate = new LocalDate()
        val futureDate = currentDate.plusYears(5)
        val input: Map[String, String] =  Map("isPropertyRevalued" -> "true",
          "partAcqDispDate.day" -> "13",
          "partAcqDispDate.month" -> "10",
          "partAcqDispDate.year" -> s"${futureDate.getYear}",
          "revaluedValue" -> "150000000",
          "revaluedDate.day" -> "12",
          "revaluedDate.month" -> "10",
          "revaluedDate.year" -> s"${futureDate.getYear}"
        )

        PropertyDetailsForms.validatePropertyDetailsRevalued(periodKey,  propertyDetailsRevaluedForm.bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length shouldBe  2
            hasErrors.errors.head.message shouldBe Messages("ated.property-details-value.partAcqDispDate.error.in-future")
            hasErrors.errors.last.message shouldBe Messages("ated.property-details-value.revaluedDate.error.in-future")
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
            hasErrors.errors.length shouldBe  1
            hasErrors.errors.head.message shouldBe Messages("ated.property-details-value.incorrect-format")
          },
          _ => {
            fail("There is some problem")
          }
        )
      }
    }
  }
}
