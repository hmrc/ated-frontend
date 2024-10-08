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

package views.html.propertyDetails

import builders.TitleBuilder
import config.ApplicationConfig
import forms.PropertyDetailsForms
import models.StandardAuthRetrievals
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.{Form, FormError}
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}
import utils.PeriodUtils

class PropertyDetailsOwnedBeforeSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals
  val injectedViewInstance: propertyDetailsOwnedBefore = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsOwnedBefore]

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  def calculatedPeriodKey(periodKey: Int): String = {PeriodUtils.calculateLowerTaxYearBoundary(periodKey).getYear.toString}

  private val form = PropertyDetailsForms.propertyDetailsOwnedBeforeForm(2014)
  override def view: Html = injectedViewInstance("",2014,  form, None, Html(""), Some("backLink"))

  "The Property Details owned before page" must {
    "have a the correct page title" in {
      doc.title mustBe TitleBuilder.buildTitle(messages("ated.property-details-value.isOwnedBeforeValuationYear.title", PeriodUtils.calculateLowerTaxYearBoundary(2014).getYear.toString))
    }
    "have the correct page header" in {
      doc.getElementsByTag("h1").text() must include (messages("ated.property-details-value.isOwnedBeforeValuationYear.title", PeriodUtils.calculateLowerTaxYearBoundary(2014).getYear.toString))
    }
    "have the correct pre heading" in {
      doc.getElementsByClass("govuk-caption-xl").text() mustBe ("This section is: " + messages("ated.property-details.pre-header"))
    }
    "have a backlink" in {
      doc.getElementsByClass("govuk-back-link").text() mustBe "Back"
    }
    "have a continue button" in {
      doc.getElementsByClass("govuk-button").text() mustBe "Save and continue"
    }
    "have a yes/no radio button" in {
      doc.getElementsByAttributeValue("for","isOwnedBeforePolicyYear").text() mustBe messages("ated.property-details-value.yes")
      doc.getElementsByAttributeValue("for","isOwnedBeforePolicyYear-2").text() mustBe messages("ated.property-details-value.no")
    }

    "have the correct error messages when no radio button is selected for 2012" in {
      val eform = Form(form.mapping, Map("isOwnedBeforePolicyYear" -> ""),
        Seq(FormError("isOwnedBeforePolicyYear", messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", calculatedPeriodKey(2012))))
        , form.value)
      def view: Html = injectedViewInstance("",2012,  eform, None, Html(""), Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.getElementById("isOwnedBeforePolicyYear-error").text() mustBe ("Error: " + messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", "2012"))
      errorDoc.getElementsByClass("govuk-error-summary__list").text() mustBe messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", "2012")
    }

    "have the correct error messages when no radio button is selected for 2017" in {
      val eform = Form(form.mapping, Map("isOwnedBeforePolicyYear" -> ""),
        Seq(FormError("isOwnedBeforePolicyYear", messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", calculatedPeriodKey(2017))))
        , form.value)
      def view: Html = injectedViewInstance("",2017,  eform, None, Html(""), Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.getElementById("isOwnedBeforePolicyYear-error").text() mustBe ("Error: " + messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", "2012"))
      errorDoc.getElementsByClass("govuk-error-summary__list").text() mustBe messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", "2012")
    }

    "have the correct error messages when no radio button is selected for 2018" in {
      val eform = Form(form.mapping, Map("isOwnedBeforePolicyYear" -> ""),
        Seq(FormError("isOwnedBeforePolicyYear", messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", calculatedPeriodKey(2018))))
        , form.value)
      def view: Html = injectedViewInstance("",2018,  eform, None, Html(""), Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.getElementById("isOwnedBeforePolicyYear-error").text() mustBe ("Error: " + messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", "2017"))
      errorDoc.getElementsByClass("govuk-error-summary__list").text() mustBe messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", "2017")
    }

    "have the correct error messages when no radio button is selected for 2022" in {
      val eform = Form(form.mapping, Map("isOwnedBeforePolicyYear" -> ""),
        Seq(FormError("isOwnedBeforePolicyYear", messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", calculatedPeriodKey(2022))))
        , form.value)
      def view: Html = injectedViewInstance("",2022,  eform, None, Html(""), Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.getElementById("isOwnedBeforePolicyYear-error").text() mustBe ("Error: " + messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", "2017"))
      errorDoc.getElementsByClass("govuk-error-summary__list").text() mustBe messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", "2017")
    }

    "have the correct error messages when no radio button is selected for 2023" in {
      val eform = Form(form.mapping, Map("isOwnedBeforePolicyYear" -> ""),
        Seq(FormError("isOwnedBeforePolicyYear", messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", calculatedPeriodKey(2023))))
        , form.value)
      def view: Html = injectedViewInstance("",2023,  eform, None, Html(""), Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.getElementById("isOwnedBeforePolicyYear-error").text() mustBe ("Error: " + messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", "2022"))
      errorDoc.getElementsByClass("govuk-error-summary__list").text() mustBe messages("ated.property-details-value.isOwnedBeforeValuationYear.error.non-selected", "2022")
    }

    "have the correct error messages" in {
      val eform = Form(form.mapping, Map("isOwnedBeforePolicyYear" -> "true"),
        Seq(FormError("ownedBeforePolicyYearValue", messages("ated.property-details-value.ownedBeforePolicyYearValue.error.empty")))
        , form.value)
      def view: Html = injectedViewInstance("",2014,  eform, None, Html(""), Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.getElementById("ownedBeforePolicyYearValue-error").text() mustBe ("Error: " + messages("ated.property-details-value.ownedBeforePolicyYearValue.error.empty"))
      errorDoc.getElementsByClass("govuk-error-summary__list").text() mustBe messages("ated.property-details-value.ownedBeforePolicyYearValue.error.empty")
    }
  }

}
