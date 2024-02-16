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
import play.api.libs.json.{JsValue, JsString, Json, Reads, Writes}
import java.time.{LocalDate, ZonedDateTime, ZoneId}
import java.time.format.DateTimeFormatter

class DateSerialisationSpec extends PlaySpec with MockitoSugar {

  // val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ") //DateTime
  // implicit val zonedDateTimeReads: Reads[ZonedDateTime] = Reads.zonedDateTimeReads(formatter)
  // implicit val zonedDateTimeWrites: Writes[ZonedDateTime] = Writes.DefaultZonedDateTimeWrites

"Date serialisation" must {
    "serialise LocalDate to standard form" in  {
      val json: JsValue = Json.toJson[LocalDate](LocalDate.of(2024, 2, 16))
      json must be(new JsString("2024-02-16"))
    }

    // "serialise DateTime to standard form" in  {
    //   val json: JsValue = Json.toJson[ZonedDateTime](ZonedDateTime.of(2024, 2, 16, 14, 17, 0, 0, ZoneId.of("Z")))
    //   json must be(new JsString("2024-02-16T14:17:00.000Z"))
    // }
  }
}
