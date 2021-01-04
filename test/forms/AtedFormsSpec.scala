/*
 * Copyright 2021 HM Revenue & Customs
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

class AtedFormsSpec extends PlaySpec with GuiceOneServerPerSuite {

  "validatePostCodeFormat" must {
    "return true for a valid postcode" in {
      AtedForms.validatePostCodeFormat(Some("ZZ1 1ZZ")) must be(true)
    }

    "return false for an invalid postcode" in {
      AtedForms.validatePostCodeFormat(Some("ZZ1")) must be(false)
    }

    "return false for an invalid postcode with ," in {
      AtedForms.validatePostCodeFormat(Some("ZZ, 1AA")) must be(false)
    }
  }

  "validateAddressLine" must {
    "return true for a blank address line" in {
      AtedForms.validateAddressLine(Some("")) must be(true)
    }

    "return true for a valid address line" in {
      AtedForms.validateAddressLine(Some("1 addressLine1")) must be(true)
    }

    "return true for a valid address line with special characters" in {
      AtedForms.validateAddressLine(Some("#12-3 Fake@Street, & Road's.")) must be(true)
    }

    "return false for an invalid address line with special characters" in {
      AtedForms.validateAddressLine(Some("<select all addresses>")) must be(false)
    }
  }

  "returnTypeForm is empty" in {
    val form = AtedForms.returnTypeForm.bind(Map.empty[String, String])
    form.fold(
      hasErrors => {
        hasErrors.errors.length mustBe 1
        hasErrors.errors.head.message mustBe "ated.summary-return.return-type.error"
      },
      _ => {
        fail("There is a problem")
      }
    )
  }
}
