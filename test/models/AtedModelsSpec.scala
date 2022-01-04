/*
 * Copyright 2022 HM Revenue & Customs
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

import builders.DisposeLiabilityReturnBuilder
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class AtedModelsSpec extends PlaySpec with GuiceOneServerPerSuite {

  "DisposeLiabilityReturn.isComplete" should {

    "return true if disposal date, address and UK bank account details have been provided" in {

        val ukBankDetails: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
          .generateDisposeLiabilityReturn("12345678901").copy(
          bankDetails = Some(
            BankDetailsModel(hasBankDetails = true, Some(BankDetails(Some(true),
              Some("Account name"), Some("87686787"), Some(SortCode("12", "12", "12")))))
        ))

        DisposeLiabilityReturn.isComplete(ukBankDetails) must be(true)

    }

    "return true if disposal date, address and non-UK bank account details have been provided" in {

      val nonUkBankDetails: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
        .generateDisposeLiabilityReturn("12345678901").copy(
        bankDetails = Some(
          BankDetailsModel(hasBankDetails = true, bankDetails = Some(BankDetails(hasUKBankAccount = Some(false),
            accountName = Some("Account name"), bicSwiftCode = Some(BicSwiftCode("87686787")), iban = Some(Iban("121212")))))
        )
        )

        DisposeLiabilityReturn.isComplete(nonUkBankDetails) must be(true)

    }

    "return false if uk bank account question has not been answered" in {

      val missingBankDetails: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder
        .generateDisposeLiabilityReturn("12345678901").copy(
        bankDetails = None
      )

      DisposeLiabilityReturn.isComplete(missingBankDetails) must be(false)

    }

  }
}
