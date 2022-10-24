package connectors

import helpers.IntegrationBase
import helpers.stubs.{AddressLookupStub, AuthAudit}
import models.{AddressLookup, AddressLookupCountry, AddressLookupRecord, AddressSearchResult}
import play.api.test.Injecting
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class AddressLookupConnectorISpec extends IntegrationBase with AuthAudit with Injecting {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector = inject[AddressLookupConnector]

  "AddressLookupConnector" when {

    "findByPostcode is called" should {

      "return a list of adresses when AL call succeeds" in {
        stubAuth()
        AddressLookupStub.postPostcodePartialSuccessResponse

        await(connector.findByPostcode(AddressLookup("BB000BB", None))) mustBe
          List(AddressLookupRecord(
            uprn = 200000698110L,
            address = AddressSearchResult(
              lines = List("2 The Test Close"),
              town = Some("Test Town"),
              county = None,
              postcode = "BB00 0BB",
              country = AddressLookupCountry(
                code = "GB",
                name = "United Kingdom"
              )
            )
          ),AddressLookupRecord(
            uprn = 200000708497L,
            address = AddressSearchResult(
              lines = List("4 Test Close"),
              town = Some("Test Town"),
              county = None,
              postcode = "BB00 0BB",
              country = AddressLookupCountry(
                code = "GB",
                name = "United Kingdom"
              )
            )
          )
          )
      }

      "return Nil if AL call returns invalid Json" in {
        stubAuth()
        AddressLookupStub.responsePostPostcode("BB000BB")(OK, s"""[{id : 1233213, "address" : {}]""")
        await(connector.findByPostcode(AddressLookup("BB000BB", None))) mustBe Nil
      }

      "return Nil if AL call returns Not Found" in {
        stubAuth()
        AddressLookupStub.responsePostPostcode("BB000BB")(NOT_FOUND, "")
        await(connector.findByPostcode(AddressLookup("BB000BB", None))) mustBe Nil
      }
    }

    "return Nil if AL call returns Internal Server Error" in {
      stubAuth()
      AddressLookupStub.responsePostPostcode("BB000BB")(INTERNAL_SERVER_ERROR, "")
      await(connector.findByPostcode(AddressLookup("BB000BB", None))) mustBe Nil
    }
  }

  "findById is called" should {

    "return OK status and an address when AL call succeeds" in {
      stubAuth()
      AddressLookupStub.postByIdSuccessResponse

      await(connector.findById("200000706253")) mustBe
        List(AddressLookupRecord(
          uprn = 200000706253L,
          address = AddressSearchResult(
            lines = List("Test House", "The Tests"),
            town = Some("Test Town"),
            county = None,
            postcode = "BB00 0BB",
            country = AddressLookupCountry(
              code = "GB",
              name = "United Kingdom"
            )
          )
        )
        )
    }

    "return Nil if AL call returns invalid Json" in {
      stubAuth()
      AddressLookupStub.responsePostUprn("21362571762")(OK, s"""[{uprn : 1233213, "address" : {}]""")
      await(connector.findById("21362571762")) mustBe Nil
    }

    "return Nil if AL call returns Not Found" in {
      stubAuth()
      AddressLookupStub.responsePostUprn("21362571762")(NOT_FOUND, "")
      await(connector.findById("21362571762")) mustBe Nil
    }

    "return Nil if AL call returns Internal Server Error" in {
      stubAuth()
      AddressLookupStub.responsePostUprn("21362571762")(INTERNAL_SERVER_ERROR, "")
      await(connector.findById("21362571762")) mustBe Nil
    }
  }
}


