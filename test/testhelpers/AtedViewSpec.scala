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

package testhelpers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html

trait AtedViewSpec extends PlaySpec with JsoupArgumentMatchers with GuiceOneServerPerSuite with MockitoSugar {
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  def view: Html

  def doc: Document = Jsoup.parse(view.toString())

  def doc(view: Html): Document = Jsoup.parse(view.toString())

  def pageWithTitle(titleText: String): Unit = {
    "have a static title" in {
      doc.title must include(titleText)
    }
  }

  def pageWithHeader(headerText: String): Unit = {
    "have a static h1 header" in {
      doc must haveHeadingWithText(headerText)
    }
  }

  def pageWithPreHeading(preHeadingText: String): Unit = {
    "have a static element with id pre-heading" in {
      val preheading = messages("ated.screen-reader.section") + " " + preHeadingText
      doc must haveElementWithIdAndText(preheading, "pre-heading")
    }
  }

  def pageWithBackLink(): Unit = {
    "have a back link" in {
      doc must haveBackLink
    }
  }

  def pageWithElementAndText(element: String, value: String): Unit = {
    s"have a id named $element with value $value" in {
      doc must haveElementWithIdAndText(value, element)
    }
  }

  def pageWithElement(element: String): Unit = {
    s"have an id named $element" in {
      doc must haveElementWithId(element)
    }
  }

  def pageWithYesNoRadioButton(
                                idYes: String,
                                idNo: String,
                                yesLabelText: String = "ated.label.yes",
                                noLabelText: String = "ated.label.no"): Unit = {
    "have a yes/no radio button" in {
      doc must haveInputLabelWithText(idYes, yesLabelText)
      doc must haveInputLabelWithText(idNo, noLabelText)
      doc.getElementById(idYes) must not be null
      doc.getElementById(idNo) must not be null

    }
  }

  def pageWithContinueButtonForm(submitUrl: String): Unit = {
    pageWithButtonForm(submitUrl, "Save and continue")
  }

  def nonBreakable(string: String): String = string.replace(" ", "\u00A0")

  def pageWithButtonForm(submitUrl: String, buttonText: String): Unit = {
    "have a form with a submit button or input labelled as buttonText" in {
      doc must haveSubmitButton(buttonText)
    }
    "have a form with the correct submit url" in {
      doc must haveFormWithSubmitUrl(submitUrl)
    }
  }
}
