/*
 * Copyright 2019 HM Revenue & Customs
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

import forms.PropertyDetailsForms.{propertyDetailsTaxAvoidanceForm, validatePropertyDetailsTaxAvoidance}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class PropertyDetailsTaxAvoidanceSpec  extends PlaySpec with MustMatchers with GuiceOneServerPerSuite {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())

  "PropertyDetailsTaxAvoidanceForm" must {
    "throw error" when {
      "form is empty" in {
        val form = propertyDetailsTaxAvoidanceForm.bind(Map.empty[String, String])
        form.fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "ated.property-details-period.isTaxAvoidance.error-field-name"
          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "taxAvoidance is selected as 'Yes' and fields are empty" in {
        val input: Map[String, String] = Map("isTaxAvoidance" -> "true")
        val form = propertyDetailsTaxAvoidanceForm.bind(input)
        validatePropertyDetailsTaxAvoidance(form).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.property-details-period.taxAvoidanceScheme.error.empty"
            hasErrors.errors(1).message mustBe "ated.property-details-period.taxAvoidancePromoterReference.error.empty"
          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "taxAvoidance is selected as 'Yes' and fields have invalid data less then 8" in {
        val input: Map[String, String] = Map("isTaxAvoidance" -> "true",
          "taxAvoidanceScheme" -> "12345",
          "taxAvoidancePromoterReference" -> "1239")
        val form = propertyDetailsTaxAvoidanceForm.bind(input)
        validatePropertyDetailsTaxAvoidance(form).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.property-details-period.taxAvoidanceScheme.error.wrong-length"
            hasErrors.errors(1).message mustBe "ated.property-details-period.taxAvoidancePromoterReference.error.wrong-length"
          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "taxAvoidance is selected as 'Yes' and fields have invalid data greater then 8" in {
        val input: Map[String, String] = Map("isTaxAvoidance" -> "true",
          "taxAvoidanceScheme" -> "123456789",
          "taxAvoidancePromoterReference" -> "123456789")
        val form = propertyDetailsTaxAvoidanceForm.bind(input)
        validatePropertyDetailsTaxAvoidance(form).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.property-details-period.taxAvoidanceScheme.error.wrong-length"
            hasErrors.errors(1).message mustBe "ated.property-details-period.taxAvoidancePromoterReference.error.wrong-length"
          },
          _ => {
            fail("There is a problem")
          }
        )
      }


      "taxAvoidance is selected as 'Yes' and fields have some random string as input" in {
        val input: Map[String, String] = Map("isTaxAvoidance" -> "true",
          "taxAvoidanceScheme" -> "asdfghto",
          "taxAvoidancePromoterReference" -> "asdfghtk")

        val form = propertyDetailsTaxAvoidanceForm.bind(input)
        validatePropertyDetailsTaxAvoidance(form).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.property-details-period.taxAvoidanceScheme.error.numbers"
            hasErrors.errors(1).message mustBe "ated.property-details-period.taxAvoidancePromoterReference.error.numbers"
          },
          _ => {
            fail("There is a problem")
          }
        )
      }
    }
  }
}
