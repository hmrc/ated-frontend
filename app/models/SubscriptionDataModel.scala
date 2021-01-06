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

import play.api.libs.json.Json


case class ContactDetails(phoneNumber: Option[String] = None,
                          mobileNumber: Option[String] = None,
                          faxNumber: Option[String] = None,
                          emailAddress: Option[String] = None)
object ContactDetails {
  implicit val formats = Json.format[ContactDetails]
}

case class EditContactDetails(firstName: String,
                              lastName: String,
                              phoneNumber: String)

object EditContactDetails {
  implicit val formats = Json.format[EditContactDetails]
}



case class EditContactDetailsEmail(
                              emailAddress: String,
                              emailConsent: Boolean)
object EditContactDetailsEmail {
  implicit val formats = Json.format[EditContactDetailsEmail]
}



case class AddressDetails(addressType: String,
                          addressLine1: String,
                          addressLine2: String,
                          addressLine3: Option[String] = None,
                          addressLine4: Option[String] = None,
                          postalCode: Option[String] = None,
                          countryCode: String)
object AddressDetails {
  implicit val formats = Json.format[AddressDetails]
}

case class Address(name1: Option[String] = None,
                   name2: Option[String] = None,
                   addressDetails: AddressDetails,
                   contactDetails: Option[ContactDetails] = None)
object Address {
  implicit val formats = Json.format[Address]
}

case class SubscriptionData(safeId: String, organisationName: String, emailConsent: Option[Boolean], address : Seq[Address])

object SubscriptionData {
  implicit val formats = Json.format[SubscriptionData]
}


case class ChangeIndicators(nameChanged: Boolean = false,
                            permanentPlaceOfBusinessChanged: Boolean = false,
                            correspondenceChanged: Boolean = false,
                            contactDetailsChanged: Boolean = false)

object ChangeIndicators {
  implicit val formats = Json.format[ChangeIndicators]
}

case class UpdateSubscriptionDataRequest(emailConsent: Boolean, changeIndicators: ChangeIndicators, address : Seq[Address])

object UpdateSubscriptionDataRequest {
  implicit val formats = Json.format[UpdateSubscriptionDataRequest]
}
