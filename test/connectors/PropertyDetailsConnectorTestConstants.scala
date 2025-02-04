/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

trait PropertyDetailsConnectorTestConstants {

  val retrieveDraftPropertyDetailsURI = "property-details/retrieve"
  val calculateDraftPropertyDetailsURI = "property-details/calculate"
  val calculateDraftChangeLiabilityURI = "liability-return/calculate"

  val submitDraftPropertyDetailsURI = "property-details/submit"

  val createDraftPropertyDetailsURI = "property-details/create"

  val saveDraftPropertyDetailsAddressRefURI = "property-details/address"
  val saveDraftPropertyDetailsTitleURI = "property-details/title"
  val saveDraftPropertyHasValueChangedURI = "property-details/has-value-change"
  val saveDraftPropertyDetailsAcquisitionURI = "property-details/acquisition"
  val saveDraftPropertyDetailsRevaluedURI = "property-details/revalued"
  val saveDraftPropertyDetailsOwnedBeforeURI = "property-details/owned-before"
  val saveDraftPropertyDetailsNewBuildURI = "property-details/new-build"
  val saveDraftPropertyDetailsNewBuildDatesURI = "property-details/new-build-dates"
  val saveDraftPropertyDetailsWhenAcquiredDatesURI = "property-details/when-acquired"
  val saveDraftPropertyDetailsNewBuildValueURI = "property-details/new-build-value"
  val saveDraftPropertyDetailsValueOnAcquisitionURI = "property-details/value-acquired"
  val saveDraftPropertyDetailsFullTaxPeriodURI = "property-details/full-tax-period"
  val saveDraftPropertyDetailsInReliefURI = "property-details/in-relief"

  val saveDraftPropertyDetailsDatesLiableURI = "property-details/dates-liable"
  val addDraftPropertyDetailsDatesLiableURI = "property-details/dates-liable/add"
  val addDraftPropertyDetailsDatesInReliefURI = "property-details/dates-in-relief/add"
  val deleteDraftPropertyDetailsPeriodURI = "property-details/period/delete"

  val deletePropertyDetailsURI = "property-details/delete"

  val saveDraftPropertyDetailsTaxAvoidanceURI = "property-details/tax-avoidance"
  val saveDraftPropertyDetailsSupportingInfoURI = "property-details/supporting-info"
  val saveDraftPropertyDetailsValuedURI = "property-details/valued"
}