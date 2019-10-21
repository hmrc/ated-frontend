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
import javax.inject.Inject
import models._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressLookupService @Inject()(addressLookupConnector: AddressLookupConnector,
                                     dataCacheConnector: DataCacheConnector) {

  val ADDRESS_LOOKUP_SEARCH_RESULTS = "ADDRESS-LOOKUP-SEARCH-RESULTS"

  def find(searchCriteria: AddressLookup)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[AddressSearchResults] = {
    addressLookupConnector.findByPostcode(searchCriteria).flatMap{
      foundData => storeSearchResults(AddressSearchResults(searchCriteria, foundData))
    }
  }

  def findById(id: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[PropertyDetailsAddress]] = {
    def convertLookupAddressToPropertyDetailsAddress(addressResult: Option[AddressLookupRecord]): Option[PropertyDetailsAddress] = {
      addressResult.flatMap(found =>
        found.address.lines match {
          case line1 :: line2 :: _ if (found.address.town.isDefined && found.address.county.isDefined) =>
            Some(PropertyDetailsAddress(line1, line2, found.address.town, found.address.county, Some(found.address.postcode)))
          case line1 :: line2 :: line3 :: _ if (found.address.county.isDefined) =>
            Some(PropertyDetailsAddress(line1, line2, Some(line3), found.address.county, Some(found.address.postcode)))
          case line1 :: line2 :: line3 :: _ if (found.address.town.isDefined) =>
            Some(PropertyDetailsAddress(line1, line2, Some(line3), found.address.town, Some(found.address.postcode)))
          case line1 :: line2 :: line3 :: line4 :: _ =>
            Some(PropertyDetailsAddress(line1, line2, Some(line3), Some(line4), Some(found.address.postcode)))
          case line1 :: line2 :: line3 :: Nil =>
            Some(PropertyDetailsAddress(line1, line2,  Some(line3), found.address.town, Some(found.address.postcode)))
          case line1 :: line2 :: Nil =>
            Some(PropertyDetailsAddress(line1, line2, found.address.town, found.address.county, Some(found.address.postcode)))
          case line1 :: Nil if (found.address.town.isDefined) =>
            found.address.town.map(town =>
              PropertyDetailsAddress(line1, town, found.address.county, None, Some(found.address.postcode))
            )
          case line1 :: Nil if (found.address.county.isDefined) =>
            found.address.county.map(county =>
              PropertyDetailsAddress(line1, county, None, None, Some(found.address.postcode))
            )
          case _ => None
        }
      )
    }
    addressLookupConnector.findById(id).map(res => convertLookupAddressToPropertyDetailsAddress(res))
  }

  def retrieveCachedSearchResults()(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[Option[AddressSearchResults]] = {
    dataCacheConnector.fetchAndGetFormData[AddressSearchResults](ADDRESS_LOOKUP_SEARCH_RESULTS)
  }


  private def storeSearchResults(searchResults: AddressSearchResults)(implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[AddressSearchResults] = {
    dataCacheConnector.saveFormData[AddressSearchResults](ADDRESS_LOOKUP_SEARCH_RESULTS, searchResults)
  }
}
