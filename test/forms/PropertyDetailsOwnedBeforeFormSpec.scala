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

import forms.PropertyDetailsForms.propertyDetailsOwnedBeforeForm
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import utils.PeriodUtils

class PropertyDetailsOwnedBeforeFormSpec extends PlaySpec with GuiceOneServerPerSuite {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())
  val periodKey = 2021
  val calculatedPeriodKey: String = PeriodUtils.calculateLowerTaxYearBoundary(periodKey).getYear.toString

  "PropertyDetailsOwnedBeforeForm" must {
    "throw error" when {
      "form is empty" in {
        val form = propertyDetailsOwnedBeforeForm(periodKey).bind(Map.empty[String, String])
        form.fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected"
            hasErrors.errors.head.args.head mustBe calculatedPeriodKey
          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "Option 'yes' is selected and ownedBefore value is empty" in {
          val input: Map[String, String] =  Map("isOwnedBeforePolicyYear" -> "true",
          "ownedBeforePolicyYearValue" -> ""
        )

        PropertyDetailsForms.validatePropertyDetailsOwnedBefore(propertyDetailsOwnedBeforeForm(periodKey).bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.last.message mustBe "ated.property-details-value.ownedBeforePolicyYearValue.error.empty"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and ownedBefore value is too high" in {
        val input: Map[String, String] =  Map("isOwnedBeforePolicyYear" -> "true",
          "ownedBeforePolicyYearValue" -> "10000000000000"
        )

        PropertyDetailsForms.validatePropertyDetailsOwnedBefore(propertyDetailsOwnedBeforeForm(periodKey).bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.last.message mustBe "ated.property-details-value.ownedBeforePolicyYearValue.error.too-high"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }


      "Option 'yes' is selected and ownedBefore value is too low" in {
        val input: Map[String, String] =  Map("isOwnedBeforePolicyYear" -> "true",
          "ownedBeforePolicyYearValue" -> "500000"
        )
        PropertyDetailsForms.validatePropertyDetailsOwnedBefore(propertyDetailsOwnedBeforeForm(periodKey).bind(input)).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.last.message mustBe "ated.property-details-value.ownedBeforePolicyYearValue.error.too-low"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }

      "Option 'yes' is selected and owned Before value is invalid" in {
        val input: Map[String, String] =  Map("isOwnedBeforePolicyYear" -> "true",
          "ownedBeforePolicyYearValue" -> "ahgfhagsfhafshg"
        )
        propertyDetailsOwnedBeforeForm(periodKey).bind(input).fold(
          hasErrors => {
            hasErrors.errors.length mustBe  1
            hasErrors.errors.head.message mustBe "ated.property-details-value.incorrect-format"
          },
          _ => {
            fail("There is some problem")
          }
        )
      }
    }
  }
}
