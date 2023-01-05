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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class PropertyDetailsFormsSpec extends PlaySpec with GuiceOneServerPerSuite {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())

  "propertyDetailsAddressForm" must {

    val propertyDetailsAddressFormData: Map[String, String] = Map(
      "line_1" -> "addressId",
      "line_2" -> "institute",
      "postcode" -> "AA1 1AA"
    )

    val propertyDetailsAddressFormDataTooLong: Map[String, String] = Map(
      "line_1" -> "a" * 36,
      "line_2" -> "a" * 36,
      "line_3" -> "a" * 36,
      "line_4" -> "a" * 36,
      "postcode" -> "a" * 11
    )

    val invalidPropertyDetailsAddressFormData: Map[String, String] = Map(
      "line_1" -> "address&-Id",
      "line_2" -> "institute&",
      "postcode" -> "AA, 1AA"
    )

    "render propertyDetailsAddressForm successfully on entering valid input data" in {
      PropertyDetailsForms.propertyDetailsAddressForm.bind(propertyDetailsAddressFormData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success => {
          success.line_1 must be("addressId")
          success.line_2 must be("institute")
          success.postcode must be(Some("AA1 1AA"))
        }
      )
    }

    "throw error on entering invalid input data" in {
      PropertyDetailsForms.propertyDetailsAddressForm.bind(invalidPropertyDetailsAddressFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("ated.error.address.postalcode.format")
          formWithErrors.errors.length must be(1)
        },
        _ => {
          fail("Form should give an error")
        }
      )
    }

    "throw error on entering address lines that have too long of a length" in {
      PropertyDetailsForms.propertyDetailsAddressForm.bind(propertyDetailsAddressFormDataTooLong).fold(
        formWithErrors => {
          formWithErrors.errors.zipWithIndex.foreach { case (error, i) =>
            val msg = if (i == 4) {
              "ated.error.address.postalcode.format"
            } else s"ated.error.address.line-${i + 1}"

            error.message must be(msg)
          }
          formWithErrors.errors.length must be(5)
        },
        _ => {
          fail("Form should give an error")
        }
      )
    }

  }

}
