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

import forms.PropertyDetailsForms._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class PropertyDetailsFormsValidationSpec extends PlaySpec with GuiceOneServerPerSuite {

  "validatedNewBuildDate" should {
    "return the correct error message when a date is missing" in {
    val testOccupyDate = "2000-07-03"
    val testRegDate = ""

    val testPropertyDetailsNewBuildDatesForm = propertyDetailsNewBuildDatesForm.bind(Map(
      "newBuildOccupyDate" -> testOccupyDate,
      "newBuildRegisterDate" -> testRegDate
    ))

    testPropertyDetailsNewBuildDatesForm.errors.last.message mustBe "ated.property-details-value-error.newBuildDates.invalidRegDateError"

    }
  }

  "validatePropertyNewBuildValue" should {
    "return the correct error message when the value is empty"  in {
      val testPropertyDetailsNewBuildValueForm = propertyDetailsNewBuildValueForm.bind(Map(
        "newBuildValue" -> ""
      ))

      testPropertyDetailsNewBuildValueForm.errors.last.message mustBe "ated.property-details-value-error.newBuildValue.emptyValue"
    }
  }

  "validatePropertyWhenAcquiredDates" should {
    "return the correct error message when the value is empty"  in {
      val testPropertyDetailsWhenAcquiredForm = propertyDetailsWhenAcquiredDatesForm.bind(Map(
        "acquiredDate" -> ""
      ))

      testPropertyDetailsWhenAcquiredForm.errors.head.message mustBe "ated.property-details-value-error.whenAcquired.invalidDateError"
    }

    "return the correct error message when the date is in the future" in {
      val testFutureDate = "2025-01-01"

      val testPropertyDetailsWhenAcquiredForm = propertyDetailsWhenAcquiredDatesForm.bind(Map(
        "acquiredDate" -> testFutureDate
      ))

      testPropertyDetailsWhenAcquiredForm.errors.last.message mustBe "ated.property-details-value-error.whenAcquired.invalidDateError"
    }

  }

  "validatePropertyDetailsValueOnAcquisition" should {
    "return the correct error message when the value is empty" in {

      val testPropertyDetailsValueAcquiredForm = propertyDetailsValueAcquiredForm.bind(Map(
        "acquiredValue" -> ""
      ))

      testPropertyDetailsValueAcquiredForm.errors.last.message mustBe "ated.property-details-value-error.valueAcquired.emptyValue"

    }
  }

}
