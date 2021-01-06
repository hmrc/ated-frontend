/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Environment

class CountryCodeUtilsSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar {

  class Setup extends CountryCodeUtils {
    val environment: Environment = app.injector.instanceOf[Environment]
  }

  "CountryCodeUtils" must {
    "getSelectedCountry" must {
      "bring the correct country from the file" in new Setup {
        getSelectedCountry("GB") must be("United Kingdom")
        getSelectedCountry("US") must be("USA")
        getSelectedCountry("VG") must be("British Virgin Islands")
        getSelectedCountry("UG") must be("Uganda")
        getSelectedCountry("zz") must be("zz")
      }
    }

    "getIsoCodeMap" must {
      "return map of country iso-code to country name" in new Setup {
        getIsoCodeTupleList must contain(("US", "USA :United States of America"))
        getIsoCodeTupleList must contain(("GB", "United Kingdom :UK, GB, Great Britain"))
        getIsoCodeTupleList must contain(("UG", "Uganda"))
      }
    }
  }
}
