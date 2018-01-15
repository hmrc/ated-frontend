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

package models

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json

class AddressLookupModelsSpec extends PlaySpec with OneServerPerSuite {

  "AddressLookup Response" should {

    "correctly parse the response for looking up an address" in {
      val jsonResponse =
        """
          |{
          |		"postcode": "ZZ1 1ZZ",
          |		"country": {
          |			"code": "UK",
          |			"name": "United Kingdom",
          |			"A": ""
          |		},
          |		"county": "Somerset",
          |		"subdivision": {
          |			"code": "GB-ENG",
          |			"name": "England",
          |			"J": ""
          |		},
          |		"K": "",
          |		"town": "Anytown",
          |		"lines": ["8 Other Place", "Some District"]
          |	}
        """.stripMargin
      val exampleJson = Json.parse(jsonResponse)
      val response = exampleJson.as[AddressSearchResult]

      response.postcode must be("ZZ1 1ZZ")
      response.town must be(Some("Anytown"))
    }
  }
}
