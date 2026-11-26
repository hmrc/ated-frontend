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

package test.helpers

import java.util.UUID

trait IntegrationConstants {

  val sessionId = s"stubbed-${UUID.randomUUID}"

  val authToken = "token"

  val period = 2019

  val atedRef = "XN1200000100001"

  val authResponseJson = {
    """
      |{"allEnrolments":[{"key":"HMRC-ATED-ORG","identifiers":[{"key":"ATEDRefNumber","value":"XN1200000100001"}],"state":"Activated"}],"affinityGroup":"Organisation","internalId":"Int-bd27f4a9-0f70-41f1-b868-73fd03eee0f6"}
      |""".stripMargin
  }

  val relief = {
    """{
      |	"processingDate": "2021-02-18",
      |	"reliefReturnResponse": [{
      |		"reliefDescription": "Dwellings opened to the public",
      |		"formBundleNumber": "1234567890"
      |	}],
      |	"liabilityReturnResponse": []
      |}
      |""".stripMargin
  }

  val userDetails = {
    """
      |{"gatewayId":"5626493794773259","authProviderId":"5626493794773259","authProviderType":"GovernmentGateway","name":"TestUser","email":"user@test.com","affinityGroup":"Organisation","credentialRole":"User","groupIdentifier":"testGroupId-8f506bf4-4cd3-4c39-8bd9-bb994c95a52f"}
      |""".stripMargin
  }

  val subscription = {
    """
      |{
      |	"safeId": "XA0001234567899",
      |	"organisationName": "ABC Limited",
      |	"address": [{
      |			"addressDetails": {
      |				"addressType": "Permanent Place Of Business",
      |				"addressLine1": "Benton Park View",
      |				"addressLine2": "Benton Park Road",
      |				"addressLine3": "Longbenton",
      |				"addressLine4": "Newcastle upon Tyne",
      |				"postalCode": "NE98 1ZZ",
      |				"countryCode": "GB"
      |			}
      |		},
      |		{
      |			"name1": "Edward",
      |			"name2": "Vedder",
      |			"addressDetails": {
      |				"addressType": "Correspondence",
      |				"addressLine1": "Melbourne House",
      |				"addressLine2": "Eastgate",
      |				"addressLine3": "Accrington",
      |				"addressLine4": "Lancashire",
      |				"postalCode": "BB5 6PU",
      |				"countryCode": "GB"
      |			},
      |			"contactDetails": {
      |				"phoneNumber": "01254 222 487",
      |				"mobileNumber": "07890346852",
      |				"emailAddress": "edtheved@pearljam.com"
      |			}
      |		}
      |	]
      |}
      |""".stripMargin
  }

  val testNavLinkJson: String =
    """
      |{
      | "home":{
      |         "en" : "Home",
      |         "url": "http://localhost:9020/business-account"
      |       },
      | "account":{
      |           "en" : "Manage account",
      |           "url" : "http://localhost:9020/business-account/manage-account"
      |       },
      | "messages":{
      |             "en" : "Messages",
      |             "url" : "http://localhost:9020/business-account/messages",
      |             "alerts": 5
      |       },
      | "help":{
      |         "en" : "Help and contact",
      |         "url" : "http://localhost:9733/business-account/help"
      |       }
      | }""".stripMargin

}
