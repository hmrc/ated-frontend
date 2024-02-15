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
import play.api.libs.json.{Json, OFormat}
// import play.api.libs.json.EnvWrites.DefaultLocalDateWrites
// import play.api.libs.json.EnvReads.DefaultLocalDateReads


case class FormBundleProperty(propertyValue: BigDecimal,dateFrom: LocalDate,dateTo: LocalDate,`type`: String,reliefDescription: Option[String])

object FormBundleProperty {
  implicit val formats: OFormat[FormBundleProperty] = Json.format[FormBundleProperty]
}

case class FormBundleAddress(addressLine1: String, addressLine2: String, addressLine3: Option[String],
                             addressLine4: Option[String], postalCode: Option[String] = None, countryCode: String)

object FormBundleAddress {
  implicit val formats: OFormat[FormBundleAddress] = Json.format[FormBundleAddress]
}


case class FormBundlePropertyDetails(titleNumber: Option[String], address: FormBundleAddress, additionalDetails: Option[String])

object FormBundlePropertyDetails {
  implicit val formats: OFormat[FormBundlePropertyDetails] = Json.format[FormBundlePropertyDetails]
}

case class FormBundleUKAccount( accountNumber: String, sortCode: String)

object FormBundleUKAccount {
  implicit val format: OFormat[FormBundleUKAccount] = Json.format[FormBundleUKAccount]
}

case class FormBundleInternationalAccount(bicSwiftCode: String, iban: String)

object FormBundleInternationalAccount {
  implicit val format: OFormat[FormBundleInternationalAccount] = Json.format[FormBundleInternationalAccount]
}

case class FormBundleBankDetails(accountName: Option[String] = None,
                                 ukAccount: Option[FormBundleUKAccount] = None,
                                 internationalAccount: Option[FormBundleInternationalAccount] = None)

object FormBundleBankDetails {
  implicit val format: OFormat[FormBundleBankDetails] = Json.format[FormBundleBankDetails]
}


case class FormBundleReturn(periodKey: String,
                            propertyDetails: FormBundlePropertyDetails,
                            dateOfAcquisition: Option[LocalDate] = None,
                            valueAtAcquisition: Option[BigDecimal] = None,
                            taxAvoidanceScheme: Option[String] = None,
                            taxAvoidancePromoterReference: Option[String] = None,
                            localAuthorityCode: Option[String] = None,
                            professionalValuation: Boolean,
                            ninetyDayRuleApplies: Boolean,
                            dateOfSubmission: LocalDate,
                            liabilityAmount: BigDecimal,
                            paymentReference: String,
                            lineItem : Seq[FormBundleProperty],
                            bankDetails: Option[FormBundleBankDetails] = None
                           )

object FormBundleReturn {
  implicit val formats: OFormat[FormBundleReturn] = Json.format[FormBundleReturn]
}
