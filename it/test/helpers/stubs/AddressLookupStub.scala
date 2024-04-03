/*
 * Copyright 2024 HM Revenue & Customs
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

package test.helpers.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.test.Helpers._
import test.helpers.IntegrationBase

object AddressLookupStub extends IntegrationBase {

  def stubPost(url: String, requestBody: Option[String] = None, status: Integer, responseBody: String): StubMapping =
    stubFor(post(urlMatching(url))
      .withRequestBody(equalToJson(requestBody.getOrElse("")))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(responseBody)
      )
    )

  def postcodeLookupRequestBody(postcode: String, filter: Option[String]): String = {
    filter match {
      case Some(f) => s"""{"postcode" : "$postcode", "filter" : "$f"}"""
      case _ => s"""{"postcode" : "$postcode"}"""
    }
  }

  def responsePostUprn(uprn: String)(status: Int, body: String): Unit =
    stubPost("/lookup/by-uprn", Some(s"""{"uprn" : "$uprn"}"""), status, body)

  def responsePostPostcode(postcode: String, filter: Option[String] = None)(status: Int, body: String): Unit = {
    stubPost("/lookup", Some(postcodeLookupRequestBody(postcode, filter)), status, body)
  }

  val partialSuccessResponse = s"""
       |[
       |    {
       |        "id": "GB200000698110",
       |        "uprn": 200000698110,
       |        "address": {
       |            "lines": [
       |                "2 The Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200000708497",
       |        "uprn": 200000708497,
       |        "address": {
       |            "lines": [
       |                "4 Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    }
       |]
       |""".stripMargin

  val fullSuccessResponseJson =
    s"""
       |[
       |    {
       |        "id": "GB200000698110",
       |        "uprn": 200000698110,
       |        "address": {
       |            "lines": [
       |                "2 The Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200000708497",
       |        "uprn": 200000708497,
       |        "address": {
       |            "lines": [
       |                "4 Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200000704710",
       |        "uprn": 200000704710,
       |        "address": {
       |            "lines": [
       |                "6 Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200000700558",
       |        "uprn": 200000700558,
       |        "address": {
       |            "lines": [
       |                "8 Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200010012154",
       |        "uprn": 200010012154,
       |        "address": {
       |            "lines": [
       |                "Test Lodge",
       |                "Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    },
       |    {
       |        "id": "GB200000706253",
       |        "uprn": 200000706253,
       |        "address": {
       |            "lines": [
       |                "Test House",
       |                "Test Close"
       |            ],
       |            "town": "Test Town",
       |            "postcode": "BB00 0BB",
       |            "subdivision": {
       |                "code": "GB-ENG",
       |                "name": "England"
       |            },
       |            "country": {
       |                "code": "GB",
       |                "name": "United Kingdom"
       |            }
       |        },
       |        "localCustodian": {
       |            "code": 1760,
       |            "name": "Test Valley"
       |        },
       |        "language": "en"
       |    }
       |]
       |""".stripMargin

  val successResponseForId =
    s"""[
       |  {
       |    "id": "GB200000706253",
       |    "uprn": 200000706253,
       |    "address": {
       |      "lines": [
       |        "Test House",
       |        "The Tests"
       |      ],
       |      "town": "Test Town",
       |      "postcode": "BB00 0BB",
       |      "subdivision": {
       |        "code": "GB-ENG",
       |        "name": "England"
       |      },
       |      "country": {
       |        "code": "GB",
       |        "name": "United Kingdom"
       |      }
       |    },
       |    "localCustodian": {
       |      "code": 1760,
       |      "name": "Test Valley"
       |    },
       |    "location": [
       |      50.9986451,
       |      -1.4690977
       |    ],
       |    "language": "en",
       |    "administrativeArea": "TEST COUNTY"
       |  }
       |]
       |""".stripMargin

  def postPostcodeFullSuccessResponse(): Unit = responsePostPostcode("BB000BB")(OK, fullSuccessResponseJson)

  def postPostcodePartialSuccessResponse(): Unit = responsePostPostcode("BB000BB")(OK, partialSuccessResponse)

  def postByIdSuccessResponse(): Unit = responsePostUprn("200000706253")(OK, successResponseForId)
}