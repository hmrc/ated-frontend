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

package config

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import play.api.mvc._


trait JavaFormat {
  val format: String
}

trait DefaultJavaFormat extends JavaFormat {
  val format: String = "yyyyMMdd"
}

trait JavaLocalDateRoutes {
  self: JavaFormat =>

  lazy val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(format)

  implicit object queryStringLocalDateBinder extends QueryStringBindable.Parsing[LocalDate](
    dateString =>
      LocalDate.from(formatter.parse(dateString)),
    _.format(formatter),
    (key: String, e: Exception) => "Cannot parse parameter %s as java.time.LocalDate: %s".format(key, e.getMessage)
  )

  implicit object pathLocalDateBinder extends PathBindable.Parsing[LocalDate](
    dateString => LocalDate.from(formatter.parse(dateString)),
    _.format(formatter),
    (key: String, e: Exception) => "Cannot parse parameter %s as java.time.LocalDate: %s".format(key, e.getMessage)
  )

}
object JavaLocalDateRoutes extends JavaLocalDateRoutes with DefaultJavaFormat
