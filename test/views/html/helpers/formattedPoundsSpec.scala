/*
 * Copyright 2021 HM Revenue & Customs
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

package views.html.helpers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}

class formattedPoundsSpec extends PlaySpec with GuiceOneAppPerTest {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = MessagesImpl(Lang("en-GB"), messagesApi)

  ".formattedPounds" should {

    "round up to the nearest full pound when greater than halfway between a pound and add a pound sign" in {

      val document: Document = Jsoup.parse(contentAsString(views.html.helpers.formattedPounds(BigDecimal(999.51))))
      document.body().text() mustBe "£1,000"
    }

    "round down to the nearest full pound when less than halfway and add a pound sign" in {

      val document: Document = Jsoup.parse(contentAsString(views.html.helpers.formattedPounds(BigDecimal(999.49))))
      document.body().text() mustBe "£999"
    }

    "round up to the nearest full pound when halfway between a pound and add a pound sign" in {

      val document: Document = Jsoup.parse(contentAsString(views.html.helpers.formattedPounds(BigDecimal(999.50))))
      document.body().text() mustBe "£1,000"
    }

  }
}
