/*
 * Copyright 2022 HM Revenue & Customs
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
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsOwnedBefore]

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  private val form = PropertyDetailsForms.propertyDetailsOwnedBeforeForm
  override def view: Html = injectedViewInstance("",2014,  form, None, Html(""), Some("backLink"))

  "The Property Details owned before page" must {
    "have a the correct page title" in {
      doc.title mustBe TitleBuilder.buildTitle(messages("ated.property-details-value.isOwnedBeforeValuationYear.title", PeriodUtils.calculateLowerTaxYearBoundary(2014).getYear.toString))
    }
    "have the correct page header" in {
      doc.title mustBe TitleBuilder.buildTitle(messages("ated.property-details-value.isOwnedBeforeValuationYear.title", PeriodUtils.calculateLowerTaxYearBoundary(2014).getYear.toString))
    }
    "have the correct pre heading" in {
      doc.title(messages("ated.property-details.pre-header"))
    }
    "have a backlink" in {
      doc.getElementsByClass("govuk-back-link").text mustBe "Back"
    }
    "have a continue button" in {
      doc.getElementsByClass("govuk-button").text mustBe "Save and continue"
    }
    "have a yes/no radio button" in {
      doc.getElementsByAttributeValue("for","isOwnedBeforePolicyYear").text() mustBe messages("ated.property-details-value.yes")
      doc.getElementsByAttributeValue("for","isOwnedBeforePolicyYear-2").text() mustBe messages("ated.property-details-value.no")
    }
    "have the correct error messages" in {
      val eform = Form(form.mapping, Map("isOwnedBeforePolicyYear" -> "true"),
        Seq(FormError("ownedBeforePolicyYearValue", messages("ated.property-details-value.ownedBeforePolicyYearValue.error.empty")))
        , form.value)
      def view: Html = injectedViewInstance("",2014,  eform, None, Html(""), Some("backLink"))
      val errorDoc = doc(view)

      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.ownedBeforePolicyYearValue.error.empty")).hasText mustBe true
      errorDoc.getElementsMatchingOwnText(messages("ated.property-details-value.ownedBeforevaluationYear.Value")).hasText mustBe true
    }
  }

}
