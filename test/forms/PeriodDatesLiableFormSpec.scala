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

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class PeriodDatesLiableFormSpec extends PlaySpec with MustMatchers with GuiceOneServerPerSuite {

  val periodKey: Int = 2016
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())

  "periodDatesLiableForm" must {
    "fail validation" when {
      "start date and end date fields not entered correctly" in {

        val inputDate = Map("startDate.day" -> "13",
          "startDate.month" -> "10",
          "startDate.year" -> "",
          "endDate.day" -> "12",
          "endDate.month" -> "10",
          "endDate.year" -> "")

        PropertyDetailsForms.periodDatesLiableForm.bind(inputDate).fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            Messages(hasErrors.errors.head.message) mustBe Messages("error.invalid.date.format")
            Messages(hasErrors.errors.last.message) mustBe Messages("error.invalid.date.format")

          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "start date and end date entered as too early" in {

        val inputDate = Map("startDate.day" -> "1",
          "startDate.month" -> "4",
          "startDate.year" -> "2014",
          "endDate.day" -> "1",
          "endDate.month" -> "8",
          "endDate.year" -> "2014")

        PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, PropertyDetailsForms.periodDatesLiableForm.bind(inputDate), periodsCheck = false)
          .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.property-details-period.datesLiable.startDate.error.too-early"
            hasErrors.errors.last.message mustBe "ated.property-details-period.datesLiable.endDate.error.too-early"

          },
          _ => {
            fail("There is a problem")
          }
        )
      }


      "start date and end date entered as too late" in {

        val inputDate = Map("startDate.day" -> "1",
          "startDate.month" -> "6",
          "startDate.year" -> "2018",
          "endDate.day" -> "11",
          "endDate.month" -> "9",
          "endDate.year" -> "2018")

        PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, PropertyDetailsForms.periodDatesLiableForm.bind(inputDate), periodsCheck = false)
          .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "ated.property-details-period.datesLiable.startDate.error.too-late"
            hasErrors.errors.last.message mustBe "ated.property-details-period.datesLiable.endDate.error.too-late"

          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "end date is before the start date" in {

        val inputDate = Map("startDate.day" -> "1",
          "startDate.month" -> "8",
          "startDate.year" -> "2016",
          "endDate.day" -> "11",
          "endDate.month" -> "4",
          "endDate.year" -> "2016")

        PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, PropertyDetailsForms.periodDatesLiableForm.bind(inputDate), periodsCheck = false)
          .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "ated.property-details-period.datesLiable.endDate.error.before-start-date"

          },
          _ => {
            fail("There is a problem")
          }
        )
      }
    }
  }
}
