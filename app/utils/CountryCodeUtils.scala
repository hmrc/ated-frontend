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

package utils

import java.util.PropertyResourceBundle
import play.api.Environment
import scala.collection.JavaConversions._
import scala.util.{Success, Try}

trait CountryCodeUtils {

  val environment: Environment

  // $COVERAGE-OFF$
  lazy val resourceStream: PropertyResourceBundle =
    (environment.resourceAsStream("country-code.properties") flatMap { stream =>
      val optBundle: Option[PropertyResourceBundle] = Try(new PropertyResourceBundle(stream)) match {
        case Success(bundle) => Some(bundle)
        case _               => None
      }
      stream.close()
      optBundle
    }).getOrElse(throw new RuntimeException("[CountryCodeUtils] Could not retrieve property bundle"))

  // $COVERAGE-ON$

  def getIsoCodeTupleList: List[(String, String)] = {
    resourceStream.getKeys.toList.map(key => (key, resourceStream.getString(key))).sortBy{case (_,v) => v}
  }


  def getSelectedCountry(isoCode: String): String = {
    def trimCountry(selectedCountry: String) = {
      val position = selectedCountry.indexOf(":")
      if (position > 0) {
        selectedCountry.substring(0, position).trim
      } else {
        selectedCountry
      }
    }

    def getCountry(isoCode: String): Option[String] = {
      val country = getIsoCodeTupleList.toMap.get(isoCode.toUpperCase)
      country.map{ selectedCountry =>
        trimCountry(selectedCountry)
      }
    }

    getCountry(isoCode.toUpperCase).fold(isoCode){x=>x}
  }

}
