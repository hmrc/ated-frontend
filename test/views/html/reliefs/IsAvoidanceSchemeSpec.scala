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

package views.html.reliefs

import config.ApplicationConfig
import forms.ReliefForms
import models.{IsTaxAvoidance, StandardAuthRetrievals}
import org.joda.time.LocalDate
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.Json
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class IsAvoidanceSchemeSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
val periodKey = 2017
  val periodStartDate = new LocalDate()
  val injectedViewInstance = app.injector.instanceOf[views.html.reliefs.avoidanceSchemeBeingUsed]

  "is avoidance scheme view" must {
    behave like pageWithTitle(messages("ated.choose-reliefs.avoidance-title"))
    behave like pageWithHeader(messages("ated.choose-reliefs.avoidance-title"))
    behave like pageWithPreHeading(messages("ated.choose-reliefs.subheader"))
    behave like pageWithBackLink
    behave like pageWithYesNoRadioButton(
      "isAvoidanceScheme-true",
      "isAvoidanceScheme-false",
      messages("ated.claim-relief.avoidance-scheme.yes"),
      messages("ated.claim-relief.avoidance-scheme.no"))
    behave like pageWithContinueButtonForm(s"/ated/reliefs/$periodKey/avoidance-schemes-used/send")
  }

  "display error" when {
    "continuing without selecting an option" in {
      val formWithErrors = ReliefForms.isTaxAvoidanceForm.bind(Json.obj("isAvoidanceScheme" -> ""))
      def view: Html = injectedViewInstance(periodKey,formWithErrors,periodStartDate,Html(""),Some("backlink"))

      val errorDoc = doc(view)

      errorDoc must haveElementAtPathWithText(".error-list", messages("ated.choose-reliefs.error.general.isAvoidanceScheme"))
      errorDoc must haveElementAtPathWithText(".error-notification", messages("ated.claim-relief.avoidance-scheme.selected"))
    }
  }


  val isTaxAvoidanceForm: Form[IsTaxAvoidance] = ReliefForms.isTaxAvoidanceForm

  override def view: Html = injectedViewInstance(periodKey, isTaxAvoidanceForm, periodStartDate, Html(""), Some("backLink"))
}
