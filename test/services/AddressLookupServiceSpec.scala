/*
 * Copyright 2019 HM Revenue & Customs
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

package services

import connectors.{AddressLookupConnector, DataCacheConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.MockAuthUtil

import scala.concurrent.Future

class AddressLookupServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {


  val mockAddressLookupConnector: AddressLookupConnector = mock[AddressLookupConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  object TestAddressLookupService extends AddressLookupService {
    override val addressLookupConnector: AddressLookupConnector = mockAddressLookupConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach: Unit = {
    reset(mockAddressLookupConnector)
    reset(mockDataCacheConnector)
  }


  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]


  "find" must {
    val address =  AddressSearchResult(List("line1", "line2"), Some("town"), Some("country"), "postCode", AddressLookupCountry("",""))
    val addressLookupRecord = AddressLookupRecord("1", address)

    "return a list of Address and cache them if we have any" in {
      val addressLookup = AddressLookup("testPostCode", None)
      val results = List(addressLookupRecord)
      val addressSearchResults = AddressSearchResults(addressLookup, results)

      when(mockAddressLookupConnector.findByPostcode(Matchers.eq(addressLookup))
        (Matchers.any())).thenReturn(Future.successful(results))
      when(mockDataCacheConnector.saveFormData(Matchers.any(), Matchers.eq(addressSearchResults))
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(addressSearchResults))

      val result = await(TestAddressLookupService.find(addressLookup))

      result.searchCriteria must be (addressLookup)
      result.results must be (List(addressLookupRecord))
    }

  }


  "findById" must {
    "return the address from the list if we have 2 lines in it" in {
      val addressLookupRecord =  AddressLookupRecord("1", AddressSearchResult(List("line1", "line2"), Some("town"), Some("county"), "postCode",
        AddressLookupCountry("","")))

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(Some(addressLookupRecord)))
      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))
      result.isDefined must be (true)
      result.get.line_1 must be ("line1")
      result.get.line_2 must be ("line2")
      result.get.line_3 must be ( Some("town"))
      result.get.line_4 must be ( Some("county"))
      result.get.postcode must be (Some("postCode"))
    }

    "return the address from the list if we have 4 lines and a town and county" in {
      val addressLookupRecord =  AddressLookupRecord("1", AddressSearchResult(List("line1", "line2", "line3", "line4"), Some("town"), Some("county"),
        "postCode", AddressLookupCountry("","")))

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(Some(addressLookupRecord)))


      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))


      result.isDefined must be (true)
      result.get.line_1 must be ("line1")
      result.get.line_2 must be ("line2")
      result.get.line_3 must be ( Some("town"))
      result.get.line_4 must be ( Some("county"))
      result.get.postcode must be (Some("postCode"))
    }

    "return the address from the list if we have 4 lines and no town or county" in {
      val addressLookupRecord =  AddressLookupRecord("1", AddressSearchResult(List("line1", "line2", "line3", "line4"), None, None, "postCode",
        AddressLookupCountry("","")))

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(Some(addressLookupRecord)))
      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))
      result.isDefined must be (true)
      result.get.line_1 must be ("line1")
      result.get.line_2 must be ("line2")
      result.get.line_3 must be ( Some("line3"))
      result.get.line_4 must be ( Some("line4"))
      result.get.postcode must be (Some("postCode"))
    }


    "return the address from the list if we have 3 lines in it and a county" in {
      val addressLookupRecord =  AddressLookupRecord("1", AddressSearchResult(List("line1", "line2", "line3"), None, Some("county"), "postCode",
        AddressLookupCountry("","")))

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(Some(addressLookupRecord)))
      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))

      result.isDefined must be (true)
      result.get.line_1 must be ("line1")
      result.get.line_2 must be ("line2")
      result.get.line_3 must be ( Some("line3"))
      result.get.line_4 must be ( Some("county"))
      result.get.postcode must be (Some("postCode"))
    }

    "return the address from the list if we have 3 lines in it and a town" in {
      val addressLookupRecord =  AddressLookupRecord("1", AddressSearchResult(List("line1", "line2", "line3"), Some("town"), None, "postCode",
        AddressLookupCountry("","")))

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(Some(addressLookupRecord)))
      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))

      result.isDefined must be (true)
      result.get.line_1 must be ("line1")
      result.get.line_2 must be ("line2")
      result.get.line_3 must be ( Some("line3"))
      result.get.line_4 must be ( Some("town"))
      result.get.postcode must be (Some("postCode"))
    }

    "return the address from the list if we have 3 lines and no town or county" in {
      val addressLookupRecord =  AddressLookupRecord("1", AddressSearchResult(List("line1", "line2", "line3"), None, None, "postCode",
        AddressLookupCountry("","")))

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(Some(addressLookupRecord)))
      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))

      result.isDefined must be (true)
      result.get.line_1 must be ("line1")
      result.get.line_2 must be ("line2")
      result.get.line_3 must be ( Some("line3"))
      result.get.line_4 must be ( None )
      result.get.postcode must be (Some("postCode"))
    }

    "return the address from the list if we have 2 lines and a town" in {
      val addressLookupRecord =  AddressLookupRecord("1", AddressSearchResult(List("line1", "line2"), Some("town"), None, "postCode",
        AddressLookupCountry("","")))

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(Some(addressLookupRecord)))
      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))

      result.isDefined must be (true)
      result.get.line_1 must be ("line1")
      result.get.line_2 must be ("line2")
      result.get.line_3 must be ( Some("town"))
      result.get.line_4 must be ( None)
      result.get.postcode must be (Some("postCode"))
    }

    "return the address from the list if we have 2 lines and a county" in {
      val addressLookupRecord =  AddressLookupRecord("1",AddressSearchResult(List("line1", "line2"), None, Some("county"), "postCode",
        AddressLookupCountry("","")))

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(Some(addressLookupRecord)))
      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))

      result.isDefined must be (true)
      result.get.line_1 must be ("line1")
      result.get.line_2 must be ("line2")
      result.get.line_3 must be ( None)
      result.get.line_4 must be ( Some("county"))
      result.get.postcode must be (Some("postCode"))
    }

    "return the address from the list if we have 1 lines and a county" in {
      val addressLookupRecord =  AddressLookupRecord("1", AddressSearchResult(List("line1"), None, Some("county"), "postCode", AddressLookupCountry("","")))

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(Some(addressLookupRecord)))
      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))

      result.isDefined must be (true)
      result.get.line_1 must be ("line1")
      result.get.line_2 must be ("county")
      result.get.line_3 must be ( None)
      result.get.line_4 must be (None)
      result.get.postcode must be (Some("postCode"))
    }

    "return the address from the list if we have 1 lines in it and a town" in {
      val addressLookupRecord =  AddressLookupRecord("1", AddressSearchResult(List("line1"), Some("town"), None, "postCode", AddressLookupCountry("","")))

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(Some(addressLookupRecord)))
      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))

      result.isDefined must be (true)
      result.get.line_1 must be ("line1")
      result.get.line_2 must be ("town")
      result.get.line_3 must be ( None)
      result.get.line_4 must be (None)
      result.get.postcode must be (Some("postCode"))
    }

    "return None if we have no line2, town or county" in {
      val address =  AddressSearchResult(List("line1"), None, None, "postCode", AddressLookupCountry("",""))
      val addressLookupRecord = AddressLookupRecord("1", address)
      val response = Some(addressLookupRecord)

      when(mockAddressLookupConnector.findById(Matchers.eq(addressLookupRecord.id))(Matchers.any())).thenReturn(Future.successful(response))

      val result = await(TestAddressLookupService.findById(addressLookupRecord.id))

      result.isDefined must be (false)
    }
  }

  "retrieveCachedSearchResults" must {
    "return the cached data" in {
      val address =  AddressSearchResult(List("line1", "line2"), Some("town"), Some("county"), "postCode", AddressLookupCountry("",""))
      val addressLookupRecord = AddressLookupRecord("1", address)

      val addressLookup = AddressLookup("testPostCode", None)
      val addressSearchResults = AddressSearchResults(addressLookup, List(addressLookupRecord) )

      when(mockDataCacheConnector.fetchAndGetFormData[AddressSearchResults](Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(addressSearchResults)))

      val result = await(TestAddressLookupService.retrieveCachedSearchResults())

      result.isDefined must be (true)
      result.get.searchCriteria must be (addressLookup)
      result.get.results must be (List(addressLookupRecord))
    }
  }

}
