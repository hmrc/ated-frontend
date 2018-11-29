/*
 * Copyright 2018 HM Revenue & Customs
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

import forms.ReliefForms
import org.joda.time.LocalDate
import play.twirl.api.Html
import utils.viewHelpers.AtedViewSpec

class IsAvoidanceSchemeSpec extends AtedViewSpec {

  val periodKey = 2017
  val periodStartDate = new LocalDate()

  "is avoidance scheme view" must {
    behave like pageWithTitle(messages("ated.choose-reliefs.avoidance-title"))
    behave like pageWithHeader(messages("ated.choose-reliefs.avoidance-title"))
    behave like pageWithPreHeading(messages("ated.choose-reliefs.subheader"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm(s"/ated/reliefs/$periodKey/avoidance-schemes-used/send")
  }


  val isTaxAvoidanceForm = ReliefForms.isTaxAvoidanceForm

  override def view: Html = views.html.reliefs.avoidanceSchemeBeingUsed(periodKey, isTaxAvoidanceForm, periodStartDate, Some("backLink"))
}
