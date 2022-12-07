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

package models

import config.ApplicationConfig
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import utils.TestModels

class SummaryReturnsModelSpec extends PlaySpec with GuiceOneServerPerSuite with TestModels with MockitoSugar {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  "SummaryReturnsModel" should {

    "read correctly from json" in {
      allReturnsJson(true, true)
        .as[SummaryReturnsModel] must be(summaryReturnsModel(periodKey = currentTaxYear))
    }

    "read correctly from json for social housing after 2020 " in {
      allReturnsJson(true, true, true)
        .as[SummaryReturnsModel] must be(summaryReturnsModel(periodKey = currentTaxYear, submittedReturnsSocialHousing = true))
    }

    "write correctly to json" in {
      Json.toJson(summaryReturnsModel(periodKey = currentTaxYear)) must be(allReturnsJson())
    }

    "write correctly to json for social housing after 2020" in {
      val jsonInput = summaryReturnsModel(periodKey = currentTaxYear, submittedReturnsSocialHousing = true)

      Json.toJson(jsonInput) must be(allReturnsJson(submittedReturnsSocialHousing = true))
    }
  }
}
