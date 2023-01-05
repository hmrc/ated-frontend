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

package testhelpers

/*
    Move a lot of repetitive tests into a separate trait. i.e "Is there a back link, submit button etc"
    - Keep this if you want or get rid of it

    Marjan
 */

import org.jsoup.nodes.Document
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

trait GovukTestHelpers extends GivenWhenThen with AnyFeatureSpecLike with GuiceOneAppPerSuite with MockitoSugar
  with MockAuthUtil {

  def checkBackLink(document: Document) = {
    Then("The back link is correct")
    assert(document.getElementsByClass("govuk-back-link").text === "Back")
    assert(document.getElementsByClass("govuk-back-link").attr("href") === "http://backLink")
  }

}
