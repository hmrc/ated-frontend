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

package models

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, JsString, JsSuccess, Json}
import java.time.{LocalDate, ZonedDateTime, ZoneId}

class DateSerialisationSpec extends PlaySpec with MockitoSugar {

"Date serialisation" must {
    "Read LocalDate to standard form" in  {
      val json: JsValue = new JsString("2024-02-16")
      json.validate[LocalDate] match {
        case JsSuccess(dte, _) if dte == LocalDate.of(2024, 2, 16) => succeed
        case _ => fail()
      }
    }

    "Read DateTime to standard form" in  {
      val json: JsValue = new JsString("2024-02-16T14:17:00.000Z")
      json.validate[ZonedDateTime] match {
        case JsSuccess(dte, _) if dte == ZonedDateTime.of(2024, 2, 16, 14, 17, 0, 0, ZoneId.of("Z")) => succeed
        case _ => fail()
      }
    }
    "Write LocalDate to standard form" in  {
      val json: JsValue = Json.toJson[LocalDate](LocalDate.of(2024, 2, 16))
      json must be(new JsString("2024-02-16"))
    }

    "Write DateTime to standard form" in  {
      val json: JsValue = Json.toJson[ZonedDateTime](ZonedDateTime.of(2024, 2, 16, 14, 17, 0, 0, ZoneId.of("Z")))
      json must be(new JsString("2024-02-16T14:17:00Z"))
    }
  }
}
