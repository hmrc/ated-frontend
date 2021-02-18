
package helpers

import java.util.UUID

trait IntegrationConstants {

  val sessionId = s"stubbed-${UUID.randomUUID}"

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

  val keystore = {
    s"""
       |{
       |	"id": "$sessionId",
       |	"data": {
       |		"get-returns-response-Id": {
       |			"atedBalance": 100.5,
       |			"allReturns": [{
       |				"periodKey": 2017,
       |				"draftReturns": [],
       |				"submittedReturns": {
       |					"periodKey": 2017,
       |					"reliefReturns": [{
       |						"formBundleNo": "123456789021",
       |						"reliefType": "Open to the public",
       |						"dateFrom": "2017-04-01",
       |						"dateTo": "2018-03-31",
       |						"dateOfSubmission": "2017-04-04",
       |						"avoidanceSchemeNumber": "01234567",
       |						"promoterReferenceNumber": "01234568"
       |					}, {
       |						"formBundleNo": "123456789022",
       |						"reliefType": "Lending",
       |						"dateFrom": "2017-04-01",
       |						"dateTo": "2018-03-31",
       |						"dateOfSubmission": "2017-04-04",
       |						"avoidanceSchemeNumber": "",
       |						"promoterReferenceNumber": ""
       |					}],
       |					"currentLiabilityReturns": [{
       |						"formBundleNo": "123456789026",
       |						"description": "332 Menlove Ave Liverpool",
       |						"liabilityAmount": 9375.12,
       |						"dateFrom": "2017-04-01",
       |						"dateTo": "2018-01-31",
       |						"dateOfSubmission": "2017-10-02",
       |						"changeAllowed": true,
       |						"paymentReference": "reference here"
       |					}],
       |					"oldLiabilityReturns": [{
       |						"formBundleNo": "123456789025",
       |						"description": "332 Menlove Ave Liverpool",
       |						"liabilityAmount": 9375.12,
       |						"dateFrom": "2017-04-01",
       |						"dateTo": "2018-01-31",
       |						"dateOfSubmission": "2017-04-10",
       |						"changeAllowed": true,
       |						"paymentReference": "reference here"
       |					}]
       |				}
       |			}, {
       |				"periodKey": 2016,
       |				"draftReturns": [],
       |				"submittedReturns": {
       |					"periodKey": 2016,
       |					"reliefReturns": [{
       |						"formBundleNo": "123456789021",
       |						"reliefType": "Farmhouses",
       |						"dateFrom": "2016-04-01",
       |						"dateTo": "2017-03-31",
       |						"dateOfSubmission": "2016-04-04",
       |						"avoidanceSchemeNumber": "01234567",
       |						"promoterReferenceNumber": "01234568"
       |					}, {
       |						"formBundleNo": "123456789022",
       |						"reliefType": "Rental businesses",
       |						"dateFrom": "2016-04-01",
       |						"dateTo": "2017-03-31",
       |						"dateOfSubmission": "2016-04-04",
       |						"avoidanceSchemeNumber": "",
       |						"promoterReferenceNumber": ""
       |					}],
       |					"currentLiabilityReturns": [{
       |						"formBundleNo": "123456789019",
       |						"description": "1 Whitehall Place Aberdeen",
       |						"liabilityAmount": 100.12,
       |						"dateFrom": "2016-05-01",
       |						"dateTo": "2017-01-01",
       |						"dateOfSubmission": "2016-05-01",
       |						"changeAllowed": true,
       |						"paymentReference": "reference here"
       |					}],
       |					"oldLiabilityReturns": [{
       |						"formBundleNo": "123456789020",
       |						"description": "1 Whitehall Place Aberdeen",
       |						"liabilityAmount": 1000.12,
       |						"dateFrom": "2017-01-02",
       |						"dateTo": "2017-03-31",
       |						"dateOfSubmission": "2016-04-01",
       |						"changeAllowed": true,
       |						"paymentReference": "reference here"
       |					}, {
       |						"formBundleNo": "123456789023",
       |						"description": "1 Whitehall Place Aberdeen",
       |						"liabilityAmount": 1000.12,
       |						"dateFrom": "2017-01-02",
       |						"dateTo": "2017-03-31",
       |						"dateOfSubmission": "2016-04-01",
       |						"changeAllowed": false,
       |						"paymentReference": "reference here"
       |					}]
       |				}
       |			}, {
       |				"periodKey": 2015,
       |				"draftReturns": [],
       |				"submittedReturns": {
       |					"periodKey": 2015,
       |					"reliefReturns": [{
       |						"formBundleNo": "123456789010",
       |						"reliefType": "Farmhouses",
       |						"dateFrom": "2015-04-01",
       |						"dateTo": "2016-03-31",
       |						"dateOfSubmission": "2013-04-04",
       |						"avoidanceSchemeNumber": "01234567",
       |						"promoterReferenceNumber": "01234568"
       |					}],
       |					"currentLiabilityReturns": [{
       |						"formBundleNo": "123456789012",
       |						"description": "1 Whitehall Place Aberdeen",
       |						"liabilityAmount": 1000.12,
       |						"dateFrom": "2016-01-02",
       |						"dateTo": "2016-03-31",
       |						"dateOfSubmission": "2014-01-01",
       |						"changeAllowed": true,
       |						"paymentReference": "reference here"
       |					}],
       |					"oldLiabilityReturns": [{
       |						"formBundleNo": "123456789024",
       |						"description": "1 Whitehall Place Aberdeen",
       |						"liabilityAmount": 1000.12,
       |						"dateFrom": "2016-01-02",
       |						"dateTo": "2016-03-31",
       |						"dateOfSubmission": "2014-01-01",
       |						"changeAllowed": false,
       |						"paymentReference": "reference here"
       |					}, {
       |						"formBundleNo": "123456789011",
       |						"description": "1 Whitehall Place Aberdeen",
       |						"liabilityAmount": 100.12,
       |						"dateFrom": "2015-04-01",
       |						"dateTo": "2016-01-01",
       |						"dateOfSubmission": "2012-01-01",
       |						"changeAllowed": true,
       |						"paymentReference": "reference here"
       |					}]
       |				}
       |			}, {
       |				"periodKey": 2014,
       |				"draftReturns": [],
       |				"submittedReturns": {
       |					"periodKey": 2014,
       |					"reliefReturns": [{
       |						"formBundleNo": "123456789013",
       |						"reliefType": "Farmhouses",
       |						"dateFrom": "2014-04-01",
       |						"dateTo": "2015-03-31",
       |						"dateOfSubmission": "2013-09-25",
       |						"avoidanceSchemeNumber": "01234567",
       |						"promoterReferenceNumber": "01234568"
       |					}],
       |					"currentLiabilityReturns": [{
       |						"formBundleNo": "123456789014",
       |						"description": "1 Lodge Lane Liverpool",
       |						"liabilityAmount": 1000.12,
       |						"dateFrom": "2014-04-01",
       |						"dateTo": "2015-03-31",
       |						"dateOfSubmission": "2013-03-03",
       |						"changeAllowed": true,
       |						"paymentReference": "reference here"
       |					}],
       |					"oldLiabilityReturns": []
       |				}
       |			}, {
       |				"periodKey": 2013,
       |				"draftReturns": [],
       |				"submittedReturns": {
       |					"periodKey": 2013,
       |					"reliefReturns": [],
       |					"currentLiabilityReturns": [{
       |						"formBundleNo": "123456789016",
       |						"description": "Campden House Terrace London",
       |						"liabilityAmount": 1000.12,
       |						"dateFrom": "2014-03-01",
       |						"dateTo": "2014-03-31",
       |						"dateOfSubmission": "2012-04-04",
       |						"changeAllowed": true,
       |						"paymentReference": "reference here"
       |					}, {
       |						"formBundleNo": "123456789015",
       |						"description": "Campden House Terrace London",
       |						"liabilityAmount": 300.33,
       |						"dateFrom": "2013-04-01",
       |						"dateTo": "2014-02-01",
       |						"dateOfSubmission": "2012-04-04",
       |						"changeAllowed": true,
       |						"paymentReference": "reference here"
       |					}, {
       |						"formBundleNo": "123456789018",
       |						"description": "1 Montpelier Square London",
       |						"liabilityAmount": 1000.12,
       |						"dateFrom": "2013-11-23",
       |						"dateTo": "2014-03-31",
       |						"dateOfSubmission": "2012-10-04",
       |						"changeAllowed": false,
       |						"paymentReference": "reference here"
       |					}],
       |					"oldLiabilityReturns": [{
       |						"formBundleNo": "123456789017",
       |						"description": "1 Montpelier Square London",
       |						"liabilityAmount": 100.12,
       |						"dateFrom": "2013-04-01",
       |						"dateTo": "2013-11-22",
       |						"dateOfSubmission": "2012-04-11",
       |						"changeAllowed": true,
       |						"paymentReference": "reference here"
       |					}]
       |				}
       |			}]
       |		},
       |		"delegatedClientAtedRefNumber": "XN1200000100001",
       |		"get-subscription-data-response-Id": {
       |			"subscriptionData": {
       |				"safeId": "XA0001234567899",
       |				"organisationName": "ABC Limited",
       |				"address": [{
       |					"addressDetails": {
       |						"addressType": "Permanent Place Of Business",
       |						"addressLine1": "Benton Park View",
       |						"addressLine2": "Benton Park Road",
       |						"addressLine3": "Longbenton",
       |						"addressLine4": "Newcastle upon Tyne",
       |						"postalCode": "NE98 1ZZ",
       |						"countryCode": "GB"
       |					}
       |				}, {
       |					"name1": "Edward",
       |					"name2": "Vedder",
       |					"addressDetails": {
       |						"addressType": "Correspondence",
       |						"addressLine1": "Melbourne House",
       |						"addressLine2": "Eastgate",
       |						"addressLine3": "Accrington",
       |						"addressLine4": "Lancashire",
       |						"postalCode": "BB5 6PU",
       |						"countryCode": "GB"
       |					},
       |					"contactDetails": {
       |						"phoneNumber": "01254 222 487",
       |						"mobileNumber": "07890346852",
       |						"emailAddress": "edtheved@pearljam.com"
       |					}
       |				}]
       |			},
       |			"registrationDetails": {
       |				"sapNumber": "0000100001",
       |				"safeId": "XA0001234567899",
       |				"isEditable": false,
       |				"isAnAgent": false,
       |				"isAnIndividual": true,
       |				"individual": {
       |					"firstName": "Callum",
       |					"lastName": "McKeefery",
       |					"dateOfBirth": "1990-04-03"
       |				},
       |				"addressDetails": {
       |					"addressLine1": "Building 9",
       |					"addressLine2": "Benton Park Road",
       |					"addressLine3": "Longbenton",
       |					"addressLine4": "Newcastle upon Tyne",
       |					"postalCode": "NE98 1YX",
       |					"countryCode": "GB"
       |				},
       |				"contactDetails": {}
       |			}
       |		},
       |		"get-selected-period-Id": {
       |			"period": "2019"
       |		},
       |		"ATED_Back_Link:ReturnTypeController": {
       |			"backLink": "/ated/period/select"
       |		},
       |		"get-selected-return-type-Id": {
       |			"returnType": "RR"
       |		},
       |		"previous-return-details-list": [],
       |		"ATED_Back_Link:ChooseReliefsController": {
       |			"backLink": "/ated/return-type/2019"
       |		},
       |		"ATED_Back_Link:AvoidanceSchemeBeingUsedController": {
       |			"backLink": "/ated/reliefs/2019/choose"
       |		},
       |		"ATED_Back_Link:AvoidanceSchemesController": {
       |			"backLink": "/ated/reliefs/2019/avoidance-schemes-used"
       |		},
       |		"ATED_Back_Link:ReliefsSummaryController": {
       |			"backLink": "/ated/reliefs/2019/avoidance-schemes-used"
       |		},
       |		"ATED_Back_Link:ReliefDeclarationController": {
       |			"backLink": "/ated/reliefs/2019/relief-summary"
       |		}
       |	},
       |	"modifiedDetails": {
       |		"createdAt": {
       |			"$$date": 1613642013005
       |		},
       |		"lastUpdated": {
       |			"$$date": 1613642037198
       |		}
       |	},
       |	"atomicId": {
       |		"$$oid": "602e391d2a0000814d5a1f2d"
       |	}
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

}
