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

package views.html.helpers

import config.ApplicationConfig
import play.api.test.Injecting
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class peakGuidanceSpec extends AtedViewSpec with MockAuthUtil with Injecting {

  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]

  val injectedView: peakGuidance = inject[views.html.helpers.peakGuidance]
  val view: Html = injectedView(true, 2020, 2019)

  "Peak guidance view" when {

    "The user is viewing returns guidance during peak" must {

      "have the current date during peak period and show correct content" in {

        assert(doc.select("strong").text() === "Warning Deadline for 2020 to 2021 returns: 30 April 2020.")
        assert(doc.select("p").first.text() === "This is the deadline for returns and payments for all " +
          "ATED-eligible properties that you own on 1 April 2020.")
        assert(doc.select("p").last().text() === "Returns for newly acquired ATED properties must be " +
          "sent to HMRC within 30 days of the date of acquisition (90 days from start date for new builds).")
      }
    }

    "The user is viewing returns outside of peak" must {

      val view: Html = injectedView(false, 2020, 2020)

      "have the current date outside peak period and show correct content" in {

        assert(doc(view).select("strong").text() === "Warning Returns for newly acquired ATED properties " +
          "must be sent to HMRC within 30 days (90 days for new builds).")
        assert(doc(view).select("p").first.text() === "Returns for 2021 to 2022 are due by 30 April 2021.")
      }
    }
  }
}
