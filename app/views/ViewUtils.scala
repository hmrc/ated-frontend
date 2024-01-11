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

package views

import play.api.data.Form
import play.api.i18n.Messages

object ViewUtils {

  def titleBuilder(title: String, form: Option[Form[_]] = None)(implicit messages: Messages): String =
    form match {
      case Some(f) if f.hasErrors || f.hasGlobalErrors =>s"${messages("error.title.prefix")} $title - ${messages("service.name")} - GOV.UK"
      case _ => s"$title - ${messages("service.name")} - GOV.UK"
    }
}
