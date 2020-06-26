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
import models.StandardAuthRetrievals
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class ReliefDeclarationSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
"Relief Declaration" must {
    behave like pageWithTitle(messages("ated.relief-summary.declaration.title"))
    behave like pageWithHeader(messages("ated.relief-summary.declaration.header"))
  }

  private val periodKey = 2017
  override def view: Html = views.html.reliefs.reliefDeclaration(periodKey, Html(""), None)

}
