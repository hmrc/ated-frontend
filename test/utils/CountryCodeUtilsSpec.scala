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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class CountryCodeUtilsSpec extends PlaySpec with GuiceOneServerPerSuite {

  "CountryCodeUtils" must {

    "getSelectedCountry" must {
      "bring the correct country from the file" in {
        CountryCodeUtils.getSelectedCountry("GB") must be("United Kingdom")
        CountryCodeUtils.getSelectedCountry("US") must be("USA")
        CountryCodeUtils.getSelectedCountry("VG") must be("British Virgin Islands")
        CountryCodeUtils.getSelectedCountry("UG") must be("Uganda")
        CountryCodeUtils.getSelectedCountry("zz") must be("zz")
      }
    }

    "getIsoCodeMap" must {
      "return map of country iso-code to country name" in {
        CountryCodeUtils.getIsoCodeTupleList must contain(("US", "USA :United States of America"))
        CountryCodeUtils.getIsoCodeTupleList must contain(("GB", "United Kingdom :UK, GB, Great Britain"))
        CountryCodeUtils.getIsoCodeTupleList must contain(("UG", "Uganda"))
      }
    }
  }

}
