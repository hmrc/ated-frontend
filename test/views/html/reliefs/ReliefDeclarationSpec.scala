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

package views.html.reliefs

import config.ApplicationConfig
import models.StandardAuthRetrievals
import play.api.test.Injecting
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class ReliefDeclarationSpec extends AtedViewSpec with Injecting with MockAuthUtil {
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val mockAppConfig: ApplicationConfig = inject[ApplicationConfig]
  val injectedView: reliefDeclaration = inject[views.html.reliefs.reliefDeclaration]
  private val periodKey = 2017
  "Relief Declaration" must {

    "have the correct page title" in {
      doc.title() mustBe "Returns declaration - GOV.UK"
    }

    "have correct heading" in {
      doc.getElementsByTag("h1").text must include("Returns declaration")
    }

    "have correct caption" in {
      doc.getElementsByClass("govuk-caption-xl").text mustBe "This section is: Create return"
    }

    "have correct submit button" in {
      doc.getElementsByClass("govuk-button").text mustBe "Agree and submit returns"
    }
  }
  override def view: Html = injectedView(periodKey, Html(""), None)
}
