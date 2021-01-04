/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain._


case class DelegationModel(attorneyName: String,
                           principalName: String,
                           link: Link,
                           accounts: PrincipalTaxIdentifiers,
                           supplementaryData: Option[Map[String,String]] = None,
                           internalId: Option[String] = None)

object DelegationModel {
  implicit val format: Format[DelegationModel] = Json.format[DelegationModel]
}


case class PrincipalTaxIdentifiers(paye: Option[Nino] = None,
                                   sa: Option[SaUtr] = None,
                                   ct: Option[CtUtr] = None,
                                   vat: Option[Vrn] = None,
                                   epaye: Option[EmpRef] = None,
                                   taxsAgent: Option[Uar] = None,
                                   ated: Option[AtedUtr] = None)

object PrincipalTaxIdentifiers {
  implicit val format: Format[PrincipalTaxIdentifiers] = Json.format[PrincipalTaxIdentifiers]
}


case class Link(text: String,
                url: String)

object Link {
  implicit val format: Format[Link] = Json.format[Link]
}
