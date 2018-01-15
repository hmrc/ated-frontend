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

import play.api.libs.json.Json

case class PreviousReturns(address: String, formBundleNumber: String)

object PreviousReturns {
  implicit val formats = Json.format[PreviousReturns]
}

case class AddressLookup(postcode: String, houseName: Option[String])

object AddressLookup {
  implicit val formats = Json.format[AddressLookup]
}

case class AddressSelected(selected: Option[String])

object AddressSelected {
  implicit val formats = Json.format[AddressSelected]
}

case class AddressLookupCountry(code: String, name: String)
object AddressLookupCountry {
  implicit val formats = Json.format[AddressLookupCountry]
}

case class AddressSearchResult(lines: List[String],
                   town: Option[String],
                   county: Option[String],
                   postcode: String,
                   country: AddressLookupCountry) {

  override def toString: String = {
    val linesToString = lines ++ List(town, county, Some(country.code)).flatten
    linesToString.mkString(", ")
  }
}

object AddressSearchResult {
  implicit val formats = Json.format[AddressSearchResult]
}

case class AddressLookupRecord(
                          id: String,
                          address: AddressSearchResult)
object AddressLookupRecord {
  implicit val formats = Json.format[AddressLookupRecord]
}


case class AddressSearchResults(searchCriteria: AddressLookup, results: Seq[AddressLookupRecord])

object AddressSearchResults {
  implicit val formats = Json.format[AddressSearchResults]
}
