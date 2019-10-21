/*
 * Copyright 2019 HM Revenue & Customs
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

package views.html

import config.ApplicationConfig
import forms.AtedForms
import models.StandardAuthRetrievals
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.MockAuthUtil
import utils.viewHelpers.AtedViewSpec

class ReturnTypeSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]

  "Return Type view" must {
    behave like pageWithTitle(messages("ated.return-type.title"))
    behave like pageWithHeader(messages("ated.return-type.header"))
    behave like pageWithPreHeading(messages("ated.return-type.pre-header"))
    behave like pageWithBackLink
    behave like pageWithButtonForm("/ated/return-type?periodKey=2014", messages("ated.return-type.button"))
    behave like pageWithYesNoRadioButton("returnType-cr", "returnType-rr",
      messages("ated.return-type.chargeable"),
      messages("ated.return-type.relief-return"))

    "check contents" in {
      doc.getElementsContainingOwnText(messages("ated.return-type.header")).hasText must be(true)
    }

    "check page errors" in {
      doc.getElementsMatchingOwnText(messages("ated.summary-return.return-type.error")).hasText mustBe true
      doc.getElementsMatchingOwnText(messages("ated.return-type.error.general.returnType")).hasText mustBe true
    }
  }

  private val form = AtedForms.returnTypeForm.withError("returnType",
    messages("ated.summary-return.return-type.error"))
  override def view: Html = views.html.returnType(2014,  form, Some("backLink"))

}
