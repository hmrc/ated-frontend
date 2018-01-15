/*
 * Copyright 2018 HM Revenue & Customs
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

package views.helpers

import play.api.data._

object FormHelpers {

  /*
   * This function finds all the errors belonging to the parent that have the child field as an argument.
   */
  def getErrors(field: Field, parent: Field): Seq[FormError] = {
    parent.errors.foldLeft(field.errors) { (errors, error) =>
      error.args.map { arg =>
        parent.name + "." + arg
      }.contains(field.name) match {
        case true => error +: errors
        case _ => errors
      }
    }
  }

  /*
   * This function finds all errors on a form which are either keyed for the field or contain the fields full path in their arguments
   */
  def getErrors(field: Field, form: Form[_]): Seq[FormError] = {
    form.errors.filter { error => error.key == field.name || error.args.contains(field.name) || field.name == error.args.fold(error.key){_ + "." + _}}
  }

  /*
   * This is a routing function to determine how to resolve the errors on a field depending on whether a parent field is passed or a form reference is in scope
   */
  def getErrors(field: Field, parent: Option[Field] = None)(implicit form: Option[Form[_]] = None): Seq[FormError] = {
    parent match {
      case Some(parent) => getErrors(field, parent)
      case _ => form match {
        case Some(form) => getErrors(field, form)
        case _ => field.errors
      }
    }
  }
}
