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

  def find(searchCriteria: AddressLookup)(implicit hc: HeaderCarrier): Future[AddressSearchResults] = {
    addressLookupConnector.findByPostcode(searchCriteria).flatMap{
      foundData => storeSearchResults(AddressSearchResults(searchCriteria, foundData))
    }
  }

  def findById(id: String)(implicit hc: HeaderCarrier): Future[Option[PropertyDetailsAddress]] = {
    def convertLookupAddressToPropertyDetailsAddress(addressResult: List[AddressLookupRecord]): Option[PropertyDetailsAddress] = {

      //The address-lookup endpoint used by findById can only ever return an array of address results with either one or zero elements

      addressResult match {
        case res :: Nil => {
          res.address.lines match {
            case line1 :: line2 :: _ if (res.address.town.isDefined && res.address.county.isDefined) =>
              Some(PropertyDetailsAddress(line1, line2, res.address.town, res.address.county, Some(res.address.postcode)))
            case line1 :: line2 :: line3 :: _ if (res.address.county.isDefined) =>
              Some(PropertyDetailsAddress(line1, line2, Some(line3), res.address.county, Some(res.address.postcode)))
            case line1 :: line2 :: line3 :: _ if (res.address.town.isDefined) =>
              Some(PropertyDetailsAddress(line1, line2, Some(line3), res.address.town, Some(res.address.postcode)))
            case line1 :: line2 :: line3 :: line4 :: _ =>
              Some(PropertyDetailsAddress(line1, line2, Some(line3), Some(line4), Some(res.address.postcode)))
            case line1 :: line2 :: line3 :: Nil =>
              Some(PropertyDetailsAddress(line1, line2,  Some(line3), res.address.town, Some(res.address.postcode)))
            case line1 :: line2 :: Nil =>
              Some(PropertyDetailsAddress(line1, line2, res.address.town, res.address.county, Some(res.address.postcode)))
            case line1 :: Nil if (res.address.town.isDefined) =>
              res.address.town.map(town =>
                PropertyDetailsAddress(line1, town, res.address.county, None, Some(res.address.postcode))
              )
            case line1 :: Nil if (res.address.county.isDefined) =>
              res.address.county.map(county =>
                PropertyDetailsAddress(line1, county, None, None, Some(res.address.postcode))
              )
            case _ => None
          }
        }
        case _ => None
      }
    }
    addressLookupConnector.findById(id).map(res => convertLookupAddressToPropertyDetailsAddress(res))
  }

  def retrieveCachedSearchResults()(implicit hc: HeaderCarrier): Future[Option[AddressSearchResults]] = {
    dataCacheConnector.fetchAndGetFormData[AddressSearchResults](ADDRESS_LOOKUP_SEARCH_RESULTS)
  }

  private def storeSearchResults(searchResults: AddressSearchResults)(implicit headerCarrier: HeaderCarrier): Future[AddressSearchResults] = {
    dataCacheConnector.saveFormData[AddressSearchResults](ADDRESS_LOOKUP_SEARCH_RESULTS, searchResults)
  }
}
