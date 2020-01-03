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

package views.html.editLiability

import config.ApplicationConfig
import forms.PropertyDetailsForms
import testhelpers.{AtedViewSpec, MockAuthUtil}
import models.StandardAuthRetrievals
import org.scalatest.mockito.MockitoSugar
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.twirl.api.Html

class EditLiabilityDatesLiableSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]


  "Edit Liability Dates Liable view" must {
    behave like pageWithTitle(messages("ated.property-details-period.change-dates-liable.title"))
    behave like pageWithHeader(messages("ated.property-details-period.change-dates-liable.header"))
    behave like pageWithPreHeading(messages("ated.property-details.pre-header-change"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/ated/liability//change/dates-liable/period/0")

    "check page contents and errors" in {

      val eform = Form(form.mapping, Map("isNewBuild" -> "true"),
        Seq(FormError("startDate", messages("ated.property-details-value.startDate.error.empty")),
        FormError("endDate", messages("ated.property-details-value.endDate.error.empty")))
        , form.value)
       def view: Html = views.html.editLiability.editLiabilityDatesLiable("",0,  eform, Some("backLink"))
      val errorDoc = doc(view)

      errorDoc must haveErrorNotification(messages("ated.property-details-value.startDate.error.empty"))
      errorDoc must haveErrorSummary(messages("ated.property-details-period.datesLiable.general.error.startDate"))
      errorDoc must haveErrorNotification(messages("ated.property-details-value.endDate.error.empty"))
      errorDoc must haveErrorSummary(messages("ated.property-details-period.datesLiable.general.error.endDate"))

      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-period.change-dates-liable.startDate")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-period.datesLiable.startDate.hint")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-period.change-dates-liable.endDate")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-period.datesLiable.endDate.hint")).hasText mustBe true

    }
  }

  private val form = PropertyDetailsForms.periodDatesLiableForm
  override def view: Html = views.html.editLiability.editLiabilityDatesLiable("",0,  form, Some("backLink"))

}
