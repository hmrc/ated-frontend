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

package forms.mappings

import forms.mappings.DateTupleCustomError.dateNotInRange
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class DateTupleCustomErrorSpec extends PlaySpec with GuiceOneServerPerSuite {
  "dateNotInRange" must {
    "return false for year of 1900" in {
      val earliestYearAccepted = 1900
      val result = dateNotInRange(earliestYearAccepted)
      result mustBe(false)
    }
    "return false for year of 2100" in {
      val latestYearAccepted = 2100
      val result = dateNotInRange(latestYearAccepted)
      result mustBe(false)
    }
    "return true for year of 1899" in {
      val tooEarlyYear = 1899
      val result = dateNotInRange(tooEarlyYear)
      result mustBe(true)
    }
    "return true for year of 2101" in {
      val tooLateYear = 2101
      val result = dateNotInRange(tooLateYear)
      result mustBe(true)
    }
  }
}
