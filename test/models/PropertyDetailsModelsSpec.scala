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

package models

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json

class PropertyDetailsModelsSpec extends PlaySpec with GuiceOneServerPerSuite {

  val ownedValue = 1599999

  "PropertyDetailsOwnedBefore Request" should {
    "read the value from isOwnedBeforePolicyYear for propertyDetailsOwnedBeforeReads" in {
      val request =
        """{ "isOwnedBeforePolicyYear": true,
          |  "ownedBeforePolicyYearValue": "1599999"
          |}""".stripMargin
      val exampleRequestJson = Json.parse(request)
      val propertyDetailsOwnedBefore = exampleRequestJson.as[PropertyDetailsOwnedBefore]
      propertyDetailsOwnedBefore.isOwnedBeforePolicyYear must be (Some(true))
      propertyDetailsOwnedBefore.ownedBeforePolicyYearValue must be (Some(ownedValue))
    }
  }

  "PropertyDetailsOwnedBefore Request" should {
    "read the value from isOwnedBefore2012 for PropertyDetailsOwnedBeforeReads" in {
      val request =
        """{  "isOwnedBefore2012": true,
          |  "ownedBefore2012Value": "1599999"
          |}""".stripMargin
      val exampleRequestJson = Json.parse(request)
      val propertyDetailsOwnedBefore = exampleRequestJson.as[PropertyDetailsOwnedBefore]
      propertyDetailsOwnedBefore.isOwnedBeforePolicyYear must be (Some(true))
      propertyDetailsOwnedBefore.ownedBeforePolicyYearValue must be (Some(ownedValue))
    }
  }

  "PropertyDetailsOwnedBefore Request" should {
    "read the value from isOwnedBeforePolicyYear and ownedBefore2012Value for PropertyDetailsOwnedBeforeReads" in {
      val request =
        """{ "isOwnedBeforePolicyYear": true,
          |  "ownedBefore2012Value": "1599999"
          |}""".stripMargin
      val exampleRequestJson = Json.parse(request)
      val propertyDetailsOwnedBefore = exampleRequestJson.as[PropertyDetailsOwnedBefore]
      propertyDetailsOwnedBefore.isOwnedBeforePolicyYear must be (Some(true))
      propertyDetailsOwnedBefore.ownedBeforePolicyYearValue must be (Some(ownedValue))
    }
  }

  "PropertyDetailsValue Request" should {
    "read the value as None for propertyDetailsValueReads" in {
      val request =
        """{
          |}""".stripMargin
      val exampleRequestJson = Json.parse(request)
      val propertyDetailsOwnedBefore = exampleRequestJson.as[PropertyDetailsOwnedBefore]
      propertyDetailsOwnedBefore.isOwnedBeforePolicyYear must be (None)
      propertyDetailsOwnedBefore.ownedBeforePolicyYearValue must be (None)
    }
  }

  }
