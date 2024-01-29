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
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class RegisteredAddressDetails(addressLine1: String,
                                    addressLine2: String,
                                    addressLine3: Option[String] = None,
                                    addressLine4: Option[String] = None,
                                    postalCode: Option[String] = None,
                                    countryCode: String)

object RegisteredAddressDetails {
  implicit val formats: OFormat[RegisteredAddressDetails] = Json.format[RegisteredAddressDetails]
}

case class RegisteredDetails(isEditable: Boolean,
                             name: String,
                             addressDetails: RegisteredAddressDetails)

object RegisteredDetails {
  implicit val formats: OFormat[RegisteredDetails] = Json.format[RegisteredDetails]
}

case class Individual(firstName: String,
                      middleName: Option[String] = None,
                      lastName: String,
                      dateOfBirth: LocalDate)

object Individual {
  implicit val formats: OFormat[Individual] = Json.format[Individual]
}


case class Organisation(organisationName: String,
                        isAGroup: Option[Boolean] = None,
                        organisationType: Option[String] = None)

object Organisation {
  implicit val formats: OFormat[Organisation] = Json.format[Organisation]
}

case class Identification(idNumber: String, issuingInstitution: String, issuingCountryCode: String)

object Identification {
  implicit val formats: OFormat[Identification] = Json.format[Identification]
}

case class EtmpRegistrationDetails(sapNumber: String,
                                   safeId: String,
                                   agentReferenceNumber: Option[String],
                                   nonUKIdentification: Option[Identification],
                                   isEditable: Boolean,
                                   isAnAgent: Boolean,
                                   isAnIndividual: Boolean,
                                   individual: Option[Individual],
                                   organisation: Option[Organisation],
                                   addressDetails: RegisteredAddressDetails,
                                   contactDetails: ContactDetails) {

  def registeredDetails: RegisteredDetails = RegisteredDetails(isEditable, name.getOrElse(""), addressDetails)

  def name: Option[String] = {
    if (isAnIndividual) individual.map(individual => s"${individual.firstName} ${individual.lastName}")
    else organisation.map(_.organisationName)
  }

}

object EtmpRegistrationDetails {
  implicit val formats: OFormat[EtmpRegistrationDetails] = Json.format[EtmpRegistrationDetails]
}

case class UpdateOrganisation(organisationName: String)

object UpdateOrganisation {
  implicit val formats: OFormat[UpdateOrganisation] = Json.format[UpdateOrganisation]
}

case class UpdateRegistrationDetailsRequest(isAnIndividual: Boolean,
                                            individual: Option[Individual],
                                            organisation: Option[UpdateOrganisation],
                                            address: RegisteredAddressDetails,
                                            contactDetails: ContactDetails,
                                            isAnAgent: Boolean,
                                            isAGroup: Boolean,
                                            identification: Option[Identification] = None) {


}

object UpdateRegistrationDetailsRequest {
  implicit val formats: OFormat[UpdateRegistrationDetailsRequest] = Json.format[UpdateRegistrationDetailsRequest]
}
