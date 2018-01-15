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

package builders

import models._
import org.joda.time.LocalDate


object RegistrationBuilder {

  def getEtmpRegistrationForOrganisation(name: String): EtmpRegistrationDetails = {
    EtmpRegistrationDetails(sapNumber = "1234567890",
      safeId = "EX0012345678909",
      agentReferenceNumber = Some("agentRefNo"),
      nonUKIdentification = Some(Identification("AAAAAAAA", "Some Place", "FR")),
      isEditable = true,
      isAnAgent = true,
      isAnIndividual = false,
      individual = None,
      organisation = Some(Organisation(name, Some(false))),
      addressDetails = RegisteredAddressDetails(addressLine1 = "addrLine1", addressLine2 = "addrLine2", countryCode = "GB"),
      contactDetails = ContactDetails())
  }

  def getEtmpRegistrationForIndividual(firstName: String, lastName: String): EtmpRegistrationDetails = {
    EtmpRegistrationDetails(sapNumber = "1234567890",
      safeId = "EX0012345678909",
      agentReferenceNumber = None,
      nonUKIdentification = None,
      isEditable = false,
      isAnAgent = true,
      isAnIndividual = true,
      individual = Some(Individual(firstName = firstName, lastName = lastName, dateOfBirth = new LocalDate("1970-01-01"))),
      organisation = None,
      addressDetails = RegisteredAddressDetails(addressLine1 = "addrLine1", addressLine2 = "addrLine2", countryCode = "GB"),
      contactDetails = ContactDetails())
  }

  def getEtmpRegistrationUpdateRequest(name: String): UpdateRegistrationDetailsRequest = {
    val oldDetails = getEtmpRegistrationForOrganisation(name)
    UpdateRegistrationDetailsRequest(isAnIndividual = oldDetails.isAnIndividual,
      individual = oldDetails.individual,
      organisation = oldDetails.organisation.map(org => UpdateOrganisation(org.organisationName)),
      address = oldDetails.addressDetails,
      contactDetails = oldDetails.contactDetails,
      isAnAgent = oldDetails.isAnAgent,
      isAGroup = oldDetails.organisation.flatMap(_.isAGroup).getOrElse(false)
    )
  }

}
