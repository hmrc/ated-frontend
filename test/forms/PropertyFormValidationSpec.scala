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


import forms.PropertyDetailsForms._
import models.{LineItem, PropertyDetailsDatesLiable}
import java.time.LocalDate
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.data.FormError
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest

class PropertyFormValidationSpec extends PlaySpec with GuiceOneServerPerSuite {

  val periodKey: Int = 2016
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())
  val dateFields = Seq(("startDate", Messages("ated.property-details-period.datesLiable.startDate.messageKey")),
    ("endDate", Messages("ated.property-details-period.datesLiable.endDate.messageKey")))

  "PropertyDetailsForm" should {

    "pass validation" when {

      "form is valid" in {
        val validPeriodDatesLiable = PropertyDetailsDatesLiable(Some(LocalDate.parse("2016-04-25")), Some(LocalDate.parse("2016-09-01")))
        val form = periodDatesLiableForm.fill(validPeriodDatesLiable)
        val boundForm = PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, form, periodsCheck = false, dateFields = dateFields)
        boundForm.errors mustBe List()
      }

      "form is valid and new period does not overlap an existing one" in {
        val validPeriodDatesLiable = PropertyDetailsDatesLiable(Some(LocalDate.parse("2016-08-02")), Some(LocalDate.parse("2016-08-31")))
        val existingPeriods = List(LineItem("liability", LocalDate.parse("2016-04-15"), LocalDate.parse("2016-08-01")),
          LineItem("liability", LocalDate.parse("2016-09-02"), LocalDate.parse("2016-10-01")))
        val form = periodDatesLiableForm.fill(validPeriodDatesLiable)
        val boundForm = PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, form, periodsCheck = true, existingPeriods, dateFields)
        boundForm.errors mustBe List()
      }
    }

    "throw error" when {

      "start date is before chargeable period" in {
        val inValidPeriodDatesLiable = PropertyDetailsDatesLiable(Some(LocalDate.parse("2016-02-25")), Some(LocalDate.parse("2016-09-01")))
        val form = periodDatesLiableForm.fill(inValidPeriodDatesLiable)
        val boundForm = PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, form, periodsCheck = false, dateFields = dateFields)
        boundForm.errors mustBe List(FormError("startDate", List("ated.property-details-period.datesLiable.startDate.error.too-early"), List()))
      }

      "start date is after chargeable period and end date is before start date" in {
        val inValidPeriodDatesLiable = PropertyDetailsDatesLiable(Some(LocalDate.parse("2017-04-25")), Some(LocalDate.parse("2016-09-01")))
        val form = periodDatesLiableForm.fill(inValidPeriodDatesLiable)
        val boundForm = PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, form, periodsCheck = false, dateFields = dateFields)
        boundForm.errors mustBe List(FormError("startDate", List("ated.property-details-period.datesLiable.startDate.error.too-late"), List()),
          FormError("endDate", List("ated.property-details-period.datesLiable.endDate.error.before-start-date"), List()))
      }

      "trying to add a period, where start date overlaps the existing period" in {
        val inValidPeriodDatesLiable = PropertyDetailsDatesLiable(Some(LocalDate.parse("2016-04-25")), Some(LocalDate.parse("2016-09-01")))
        val existingPeriods = List(LineItem("liability", LocalDate.parse("2016-04-15"), LocalDate.parse("2016-08-01")))
        val form = periodDatesLiableForm.fill(inValidPeriodDatesLiable)
        val boundForm = PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, form, periodsCheck = true, existingPeriods, dateFields)
        boundForm.errors mustBe List(FormError("startDate", List("ated.property-details-period.datesLiable.overlap.error"), List()))
      }

      "trying to add a period, where end date overlaps the existing period" in {
        val inValidPeriodDatesLiable = PropertyDetailsDatesLiable(Some(LocalDate.parse("2016-04-13")), Some(LocalDate.parse("2016-07-01")))
        val existingPeriods = List(LineItem("liability", LocalDate.parse("2016-04-15"), LocalDate.parse("2016-08-01")))
        val form = periodDatesLiableForm.fill(inValidPeriodDatesLiable)
        val boundForm = PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, form, periodsCheck = true, existingPeriods, dateFields)
        boundForm.errors mustBe List(FormError("endDate", List("ated.property-details-period.datesLiable.overlap.error"), List()))
      }

      "trying to add a period, where the dates encompass an existing period" in {
        val inValidPeriodDatesLiable = PropertyDetailsDatesLiable(Some(LocalDate.parse("2016-04-13")), Some(LocalDate.parse("2016-10-02")))
        val existingPeriods = List(LineItem("liability", LocalDate.parse("2016-04-15"), LocalDate.parse("2016-08-01")),
          LineItem("liability", LocalDate.parse("2016-08-02"), LocalDate.parse("2016-10-01")))
        val form = periodDatesLiableForm.fill(inValidPeriodDatesLiable)
        val boundForm = PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, form, periodsCheck = true, existingPeriods, dateFields)
        boundForm.errors mustBe List(FormError("startDate", List("ated.property-details-period.datesLiable.overlap.error"), List()),
          FormError("endDate", List("ated.property-details-period.datesLiable.overlap.error"), List()))
      }

      "trying to add a period, where the start date overlap/encompass an existing period" in {
        val inValidPeriodDatesLiable = PropertyDetailsDatesLiable(Some(LocalDate.parse("2016-04-15")), Some(LocalDate.parse("2016-10-02")))
        val existingPeriods = List(LineItem("liability", LocalDate.parse("2016-04-15"), LocalDate.parse("2016-08-01")),
          LineItem("liability", LocalDate.parse("2016-08-02"), LocalDate.parse("2016-10-01")))
        val form = periodDatesLiableForm.fill(inValidPeriodDatesLiable)
        val boundForm = PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, form, periodsCheck = true, existingPeriods, dateFields)
        boundForm.errors mustBe List(FormError("startDate", List("ated.property-details-period.datesLiable.overlap.error"), List()))
      }

      "trying to add a period, where the end date overlap/encompass an existing period" in {
        val inValidPeriodDatesLiable = PropertyDetailsDatesLiable(Some(LocalDate.parse("2016-04-13")), Some(LocalDate.parse("2016-10-01")))
        val existingPeriods = List(LineItem("liability", LocalDate.parse("2016-04-15"), LocalDate.parse("2016-08-01")),
          LineItem("liability", LocalDate.parse("2016-08-02"), LocalDate.parse("2016-10-01")))
        val form = periodDatesLiableForm.fill(inValidPeriodDatesLiable)
        val boundForm = PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, form, periodsCheck = true, existingPeriods, dateFields)
        boundForm.errors mustBe List(FormError("endDate", List("ated.property-details-period.datesLiable.overlap.error"), List()))
      }

      "end date is before chargeable period and start date" in {
        val inValidPeriodDatesLiable = PropertyDetailsDatesLiable(Some(LocalDate.parse("2016-05-25")), Some(LocalDate.parse("2016-02-01")))
        val form = periodDatesLiableForm.fill(inValidPeriodDatesLiable)
        val boundForm = PropertyDetailsForms.validatePropertyDetailsDatesLiable(periodKey, form, periodsCheck = false, dateFields = dateFields)
        boundForm.errors mustBe List(FormError("endDate",
          List("ated.property-details-period.datesLiable.endDate.error.before-start-date-and-too-early"), List()))
      }

      "propertyDetailsAcquisitionForm is empty" in {
        val form = propertyDetailsAcquisitionForm.bind(Map.empty[String, String])
        form.fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "ated.property-details-value.anAcquisition.error-field-name"

          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "periodsInAndOutReliefForm is empty" in {
        val form = periodsInAndOutReliefForm.bind(Map.empty[String, String])
        form.fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "ated.property-details-period.isInRelief.error-field-name"

          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "propertyDetailsProfessionallyValuedForm is empty" in {
        val form = propertyDetailsProfessionallyValuedForm.bind(Map.empty[String, String])
        form.fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "ated.property-details-value.isValuedByAgent.error.non-selected"
          },
          _ => {
            fail("There is a problem")
          }
        )
      }
    }
  }
}
