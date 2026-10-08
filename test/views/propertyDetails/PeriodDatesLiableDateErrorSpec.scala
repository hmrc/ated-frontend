/*
 * Copyright 2024 HM Revenue & Customs
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

package views.propertyDetails

import config.ApplicationConfig
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms.periodDatesLiableForm
import models.{PropertyDetailsDatesLiable, StandardAuthRetrievals}
import play.api.data.Form
import play.twirl.api.{Html, HtmlFormat}
import testhelpers.{AtedViewSpec, MockAuthUtil}
import views.html.propertyDetails.periodDatesLiable

class PeriodDatesLiableDateErrorSpec extends AtedViewSpec with MockAuthUtil {

  given appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  given authContext: StandardAuthRetrievals = organisationStandardRetrievals

  val periodKey = 2026

  val injectedViewInstance: periodDatesLiable = app.injector.instanceOf[views.html.propertyDetails.periodDatesLiable]

  val dateFields: Seq[(String, String)] = Seq(
    ("startDate", messages("ated.property-details-period.datesLiable.startDate.messageKey")),
    ("endDate", messages("ated.property-details-period.datesLiable.endDate.messageKey"))
  )

  override def view: Html = viewWith(periodDatesLiableForm)

  def viewWith(form: Form[PropertyDetailsDatesLiable]): Html =
    injectedViewInstance("anything", periodKey, form, "ated.property-details-period.datesLiable.title", None, None, HtmlFormat.empty, Some("localhost"))


  /** A start date with a day only, and a complete end date. */
  val startDateMissingMonthAndYear: Form[PropertyDetailsDatesLiable] =
    PropertyDetailsForms.validatePropertyDetailsDatesLiable(
      periodKey,
      periodDatesLiableForm.bind(Map(
        "startDate.day" -> "1", "startDate.month" -> "", "startDate.year" -> "",
        "endDate.day" -> "31", "endDate.month" -> "3", "endDate.year" -> (periodKey + 1).toString
      )),
      periodsCheck = false,
      dateFields = dateFields
    )

  /** A start date whose year falls outside the accepted 1900-2100 range. */
  val startDateYearOutOfRange: Form[PropertyDetailsDatesLiable] =
    PropertyDetailsForms.validatePropertyDetailsDatesLiable(
      periodKey,
      periodDatesLiableForm.bind(Map(
        "startDate.day" -> "1", "startDate.month" -> "4", "startDate.year" -> "1899",
        "endDate.day" -> "31", "endDate.month" -> "3", "endDate.year" -> (periodKey + 1).toString
      )),
      periodsCheck = false,
      dateFields = dateFields
    )

  "The dates liable view" when {

    "rendered with a valid form" must {
      "style none of the inputs as errors" in {
        doc.getElementById("startDate.day").className() must not include "govuk-input--error"
        doc.getElementById("startDate.month").className() must not include "govuk-input--error"
        doc.getElementById("startDate.year").className() must not include "govuk-input--error"
      }
    }

    "the start date has a day but no month or year" must {

      "raise the month and year error against the start date month key" in {
        startDateMissingMonthAndYear.errors.map(e => (e.key, e.message)) mustBe
          Seq(("startDate.month", "ated.error.date.monthyear.missing"))
      }

      "style both the start date month and year inputs as errors" in {
        doc(viewWith(startDateMissingMonthAndYear)).getElementById("startDate.month").className() must include("govuk-input--error")
        doc(viewWith(startDateMissingMonthAndYear)).getElementById("startDate.year").className() must include("govuk-input--error")
      }

      "leave the start date day unstyled" in {
        doc(viewWith(startDateMissingMonthAndYear)).getElementById("startDate.day").className() must not include "govuk-input--error"
      }

      "leave the end date inputs unstyled" in {
        doc(viewWith(startDateMissingMonthAndYear)).getElementById("endDate.day").className() must not include "govuk-input--error"
        doc(viewWith(startDateMissingMonthAndYear)).getElementById("endDate.month").className() must not include "govuk-input--error"
        doc(viewWith(startDateMissingMonthAndYear)).getElementById("endDate.year").className() must not include "govuk-input--error"
      }
    }

    "the start date year is outside the accepted range" must {

      "raise the out of range error against the start date year key" in {
        startDateYearOutOfRange.errors.map(e => (e.key, e.message)) mustBe
          Seq(("startDate.year", "ated.error.date.notInRange"))
      }

      "style the start date year as an error" in {
        doc(viewWith(startDateYearOutOfRange)).getElementById("startDate.year").className() must include("govuk-input--error")
      }

      "leave the start date day and month unstyled" in {
        doc(viewWith(startDateYearOutOfRange)).getElementById("startDate.day").className() must not include "govuk-input--error"
        doc(viewWith(startDateYearOutOfRange)).getElementById("startDate.month").className() must not include "govuk-input--error"
      }
    }
  }
}
