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

import java.time.LocalDate
import java.time.format.DateTimeFormatter


trait StringFormatting[T] {
  def toString(format: String)(value: T): String
}

object StringFormatting {
  implicit val stringformatting: StringFormatting[LocalDate] = new StringFormatting[LocalDate]{
      def toString(format: String)(value: LocalDate): String = value.format(DateTimeFormatter.ofPattern(format))
  }

  // Syntax
  implicit class StringFormattingOps[A](value: A) {
    def toString(format: String)(implicit a: StringFormatting[A]): String = a.toString(format)(value)
  }
}
