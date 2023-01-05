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

package views.html.editLiability

import config.ApplicationConfig
import forms.PropertyDetailsForms
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.{Form, FormError}
import play.api.mvc.ControllerHelpers.TODO
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class EditLiabilityDatesLiableSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance = app.injector.instanceOf[views.html.editLiability.editLiabilityDatesLiable]
  private val form = PropertyDetailsForms.periodDatesLiableForm
  override def view: Html = injectedViewInstance("",0,  form, Html(""), Some("backLink"))
  override def doc: Document = Jsoup.parse(view.toString())
  override def doc(view: Html): Document = Jsoup.parse(view.toString())
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
"Edit Liability Dates Liable view" must {
    behave like pageWithTitle(messages("ated.property-details-period.change-dates-liable.title"))
  doc.getElementsByTag("h1").text.contains(messages("ated.property-details-period.change-dates-liable.header"))
  doc.getElementsByTag("h1").text.contains(messages("ated.property-details.pre-header-change"))
  doc.getElementsByClass("govuk-back-link").text === "Back"
  doc.getElementsByClass("govuk-back-link").attr("href") === "http://backLink"
    behave like pageWithContinueButtonForm("/ated/liability//change/dates-liable/period/0")

    "check page contents and errors" in {

      val eform = Form(form.mapping, Map("isNewBuild" -> "true"),
        Seq(FormError("startDate", messages("ated.property-details-value.startDate.error.empty")),
        FormError("endDate", messages("ated.property-details-value.endDate.error.empty")))
        , form.value)
       def view: Html = injectedViewInstance("",0,  eform, Html(""), Some("backLink"))
      val errorDoc = doc(view)
      TODO
      errorDoc.getElementsByClass("govuk-error-message").text.contains(messages("ated.property-details-value.startDate.error.empty"))
      errorDoc.getElementsByClass("govuk-error-summary__list").text.contains(messages("ated.property-details-value.startDate.error.empty"))
      errorDoc.getElementsByClass("govuk-error-message").text.contains(messages("ated.property-details-value.endDate.error.empty"))
      errorDoc.getElementsByClass("govuk-error-summary__list").text.contains(messages("ated.property-details-period.datesLiable.general.error.endDate"))

      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-period.change-dates-liable.startDate")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-period.datesLiable.startDate.hint")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-period.change-dates-liable.endDate")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-period.datesLiable.endDate.hint")).hasText mustBe true

    }
  }



}
