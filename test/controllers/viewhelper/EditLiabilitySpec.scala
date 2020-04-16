/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.viewhelper


import org.scalatestplus.play.PlaySpec
import utils.AtedConstants

class EditLiabilitySpec extends PlaySpec {
  val headerPrefix = "Foo"

  "createHeaderMessages function" must {
    "return header for further view" in {

      EditLiability.createHeadermessages(AtedConstants.Further, headerPrefix) mustBe "Foo.further"
    }

    "return header for amend view" in {

      EditLiability.createHeadermessages(AtedConstants.Amend, headerPrefix) mustBe "Foo.amend"
    }

    "return header for change view" in {

      EditLiability.createHeadermessages(AtedConstants.Change, headerPrefix) mustBe "Foo.change"
    }
  }

  "returnType" must {

    "return type as Further" in {
      EditLiability.returnTypeFromAmount(BigDecimal(1)) mustBe AtedConstants.Further
    }

    "return type as Amend" in {
      EditLiability.returnTypeFromAmount(BigDecimal(-1)) mustBe AtedConstants.Amend
    }

    "return type as Change" in {
      EditLiability.returnTypeFromAmount(BigDecimal(0)) mustBe AtedConstants.Change
    }
  }
}
