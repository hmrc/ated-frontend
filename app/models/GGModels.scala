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

package models

import play.api.libs.json.Json

case class IdentifierForDisplay(`type`: String, value: String)

object IdentifierForDisplay {
  implicit val formats = Json.format[IdentifierForDisplay]
}

case class RetrieveClientAllocation(friendlyName: String, identifiersForDisplay: List[IdentifierForDisplay])

object RetrieveClientAllocation {
  implicit val formats = Json.format[RetrieveClientAllocation]
}

case class Identifier(identifierType: String, value: String)

object Identifier {
  implicit val format = Json.format[Identifier]
}

case class ClientToAssign(serviceName: String, identifiers: List[Identifier])

object ClientToAssign {
  implicit val format = Json.format[ClientToAssign]
}

case class AssignClientAllocation(clientAllocation: ClientToAssign)

object AssignClientAllocation {
  implicit val format = Json.format[AssignClientAllocation]
}

case class ClientToSetFriendlyName(serviceName: String, identifiers: List[Identifier], newFriendlyName:String)

object ClientToSetFriendlyName {
  implicit val format = Json.format[ClientToSetFriendlyName]
}

case class SetFriendlyNameClientAllocation(clientAllocation: ClientToSetFriendlyName)

object SetFriendlyNameClientAllocation {
  implicit val format = Json.format[SetFriendlyNameClientAllocation]
}
