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

import builders.{ChangeLiabilityReturnBuilder, PropertyDetailsBuilder}
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.FakeRequest

import scala.collection.mutable.ArrayBuffer

class AtedUtilsSpec extends PlaySpec with GuiceOneServerPerSuite {

  "AtedUtils" must {
    "patternCheckARN should validate input ARN" in {
      AtedUtils.patternCheckARN("JARN1234567") must be(true)
      AtedUtils.patternCheckARN("jarn1234567") must be(true)
      AtedUtils.patternCheckARN("jArn1234567") must be(true)
      AtedUtils.patternCheckARN("JaRn1234567") must be(true)
      AtedUtils.patternCheckARN("JARN123456") must be(false)
      AtedUtils.patternCheckARN("1ARN1234567") must be(false)
      AtedUtils.patternCheckARN("11RN1234567") must be(false)
    }

    "isValidARN validates pattern matched ARN" in {
      AtedUtils.isValidARN("12345678901") must be(false)
      AtedUtils.isValidARN("AARN1111110") must be(false)
      AtedUtils.isValidARN("BARN1111111") must be(true)
      AtedUtils.isValidARN("FARN1111112") must be(true)
      AtedUtils.isValidARN("JARN1111113") must be(true)
      AtedUtils.isValidARN("NARN1111114") must be(true)
      AtedUtils.isValidARN("RARN1111115") must be(true)
      AtedUtils.isValidARN("VARN1111116") must be(true)

      AtedUtils.isValidARN("AARN0000002") must be(true)
      AtedUtils.isValidARN("BARN0000008") must be(true)
      AtedUtils.isValidARN("CARN0000017") must be(true)
      AtedUtils.isValidARN("DARN0000026") must be(true)
      AtedUtils.isValidARN("EARN0000003") must be(true)
      AtedUtils.isValidARN("FARN0000009") must be(true)
      AtedUtils.isValidARN("GARN0000018") must be(true)
      AtedUtils.isValidARN("HARN0000027") must be(true)
      AtedUtils.isValidARN("JARN0000013") must be(true)
      AtedUtils.isValidARN("KARN0000019") must be(true)
      AtedUtils.isValidARN("LARN0000028") must be(true)
      AtedUtils.isValidARN("MARN0000005") must be(true)
      AtedUtils.isValidARN("NARN0000014") must be(true)
      AtedUtils.isValidARN("PARN0000000") must be(true)
      AtedUtils.isValidARN("QARN0000006") must be(true)
      AtedUtils.isValidARN("RARN0000015") must be(true)
      AtedUtils.isValidARN("SARN0000024") must be(true)
      AtedUtils.isValidARN("TARN0000001") must be(true)
      AtedUtils.isValidARN("VARN0000016") must be(true)
      AtedUtils.isValidARN("WARN0000025") must be(true)
      AtedUtils.isValidARN("XARN0000004") must be(true)
      AtedUtils.isValidARN("YARN0000023") must be(true)
      AtedUtils.isValidARN("ZARN0000007") must be(true)
    }
  }

  "format Post Code" must {
    "return None if we have no post code" in {
      AtedUtils.formatPostCode(None) must be(None)
    }

    "add a space into the correct position when the post code is 6 characters" in {
      AtedUtils.formatPostCode(Some("XX1 1XX")) must be(Some("XX1 1XX"))
      AtedUtils.formatPostCode(Some("XX11XX")) must be(Some("XX1 1XX"))
      AtedUtils.formatPostCode(Some("  XX11XX    ")) must be(Some("XX1 1XX"))
    }
    "add a space into the correct position when the post code is 7 characters" in {
      AtedUtils.formatPostCode(Some("XX11 1XX")) must be(Some("XX11 1XX"))
      AtedUtils.formatPostCode(Some("XX111XX")) must be(Some("XX11 1XX"))
      AtedUtils.formatPostCode(Some("   XX 111XX    ")) must be(Some("XX11 1XX"))
    }
    "the case should be correct for the postcode" in {
      AtedUtils.formatPostCode(Some("xx1 1xx")) must be(Some("XX1 1XX"))
    }
  }


  "test Edit journey modes" must {
    "return Edit Submitted if we have a formBundle" in {
      val propertyDetails = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("1")
      AtedUtils.getEditSubmittedMode(propertyDetails) must be(Some(AtedUtils.EDIT_SUBMITTED))
    }

    "return Edit Prev Return if we have a prev return" in {
      val propertyDetails = ChangeLiabilityReturnBuilder.generateChangeLiabilityReturn("1")
      AtedUtils.getEditSubmittedMode(propertyDetails, Some(true)) must be(Some(AtedUtils.EDIT_PREV_RETURN))
    }


    "return None if we have no formBundle" in {
      val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1")
      AtedUtils.getEditSubmittedMode(propertyDetails).isDefined must be(false)
    }
  }


  "canSubmit" must {
    "return true if periodKey is less than the current year" in {
      val currentDate = new LocalDate(2017, 3, 1)
      AtedUtils.canSubmit(2015, currentDate) must be(true)
    }

    "return true if periodKey is in the current year" in {
      val currentDate = new LocalDate(2017, 3, 1)
      AtedUtils.canSubmit(2016, currentDate) must be(true)
    }

    "return false if periodKey is later than the current year" in {
      val currentDate = new LocalDate(2016, 4, 1)
      AtedUtils.canSubmit(2017, currentDate) must be(false)
    }
  }

  "test edit summary back link" must {
    "return the edit chargeable back link if this is an edit mode" in {
      val backLink = AtedUtils.getSummaryBackLink("testId", Some(AtedUtils.EDIT_SUBMITTED))
      backLink.isDefined must be(true)
      backLink.get must include("/liability/testId/change/summary")
    }

    "return the chargeable back link if this is not in edit mode" in {
      val backLink = AtedUtils.getSummaryBackLink("testId", None)
      backLink.isDefined must be(true)
      backLink.get must include("/liability/create/summary/testId")
    }
  }


  "masking the UK sortCode" must {

    "fail and return blank" when {
      "blank input is passed" in {
        AtedUtils.maskSortCode("") must be("")
      }
      "input less than six characters is passed" in {
        AtedUtils.maskSortCode("12345") must be("")
      }
    }

    "succeed and return the masked sort code" when {
      "input more than six characters is passed" in {
        AtedUtils.maskSortCode("123456") must be("XX - XX - 56")
      }
    }

  }

  "masking the bank account details" must {

    "fail and return blank" when {
      "blank input is passed" in {
        AtedUtils.maskBankDetails("", 4) must be("")
      }
      "input less than 5five characters is passed" in {
        AtedUtils.maskBankDetails("1234", 4) must be("")
      }
    }

    "succeed and return the masked sort code" when {
      "input more than six characters is passed and pos is 4" in {
        AtedUtils.maskBankDetails("123456", 4) must be("XX3456")
      }

      "input more than six characters is passed and pos is 2" in {
        AtedUtils.maskBankDetails("123456", 2) must be("XXXX56")
      }
    }
  }

  "adds the new parameter in the request data" in {
    implicit val request = FakeRequest()
    AtedUtils.addParamsToRequest(Map("periodKey" -> ArrayBuffer("2017"))) must be(None)
  }

  "print Not Provided" when {

    "string is blank/empty" in {
      AtedUtils.printNotProvidedIfEmpty("") must be("ated.property-details-summary.field-blank")
    }
  }

   "print the string" when {

     "string is not blank/empty" in {
       AtedUtils.printNotProvidedIfEmpty("abc") must be("abc")
     }
   }

  "createLables" should {

    "create a legitimate labels" when {

      "correct form label is passed" in {
        AtedUtils.createLabel("sortCode.firstElement") must be("First two numbers")
        AtedUtils.createLabel("sortCode.secondElement") must be("Second two numbers")
        AtedUtils.createLabel("sortCode.thirdElement") must be("Third two numbers")
      }

      "create no label for incorrect form labels" in {
        AtedUtils.createLabel("hello") must be("")
      }
    }
  }
}
