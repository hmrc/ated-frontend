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

package connectors

import config.ApplicationConfig

import javax.inject.Inject
import models._
import java.time.LocalDate
import play.api.libs.json
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import play.api.libs.json.Writes._

import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsConnector @Inject()(appConfig: ApplicationConfig,
                                         http: DefaultHttpClient)
                                        (implicit ec: ExecutionContext)extends RawResponseReads {

  val serviceURL: String = appConfig.conf.baseUrl("ated") + "/ated/"
  val baseURI = "ated"
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

  def createDraftPropertyDetails(periodKey: Int, propertyDetails: PropertyDetailsAddress)
                                        (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$createDraftPropertyDetailsURI/$periodKey"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }


  def saveDraftPropertyDetailsAddressRef(id: String, propertyDetails: PropertyDetailsAddress)
                                        (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsAddressRefURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsTitle(id: String, propertyDetails: PropertyDetailsTitle)
                                        (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsTitleURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftHasValueChanged(id: String, propertyDetails: Boolean)
                                   (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyHasValueChangedURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsAcquisition(id: String, overLimit: Boolean)
                                   (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsAcquisitionURI/$id"""
    val jsonData = Json.toJson(overLimit)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsRevalued(id: String, propertyDetails: PropertyDetailsRevalued)
                                         (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsRevaluedURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsOwnedBefore(id: String, propertyDetails: PropertyDetailsOwnedBefore)
                                      (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsOwnedBeforeURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsProfessionallyValued(id: String, propertyDetails: PropertyDetailsProfessionallyValued)
                                      (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsValuedURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsNewBuild(id: String, propertyDetails: PropertyDetailsNewBuild)
                                                  (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsNewBuildURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailNewBuildDates(id: String, propertyDetailsNewBuildDates: PropertyDetailsNewBuildDates)
                                          (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsNewBuildDatesURI/$id"""
    val jsonData = Json.toJson(propertyDetailsNewBuildDates)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsWhenAcquiredDates(id: String, propertyDetailsWhenAcquiredDates: PropertyDetailsWhenAcquiredDates)
                                               (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsWhenAcquiredDatesURI/$id"""
    val jsonData = Json.toJson(propertyDetailsWhenAcquiredDates)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsNewBuildValue(id: String, propertyDetailsNewBuildValue: PropertyDetailsNewBuildValue)
                                           (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsNewBuildValueURI/$id"""
    val jsonData = Json.toJson(propertyDetailsNewBuildValue)
    http.POST[json.JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsValueAcquired(id: String, propertyDetailsValueOnAcquisition: PropertyDetailsValueOnAcquisition)
                                           (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsValueOnAcquisitionURI/$id"""
    val jsonData = Json.toJson(propertyDetailsValueOnAcquisition)
    http.POST[json.JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftIsFullTaxPeriod(id: String, isFullPeriod: IsFullTaxPeriod)
                                      (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsFullTaxPeriodURI/$id"""
    val jsonData = Json.toJson(isFullPeriod)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsInRelief(id: String, propertyDetails: PropertyDetailsInRelief)
                                      (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsInReliefURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsDatesLiable(id: String, propertyDetails: PropertyDetailsDatesLiable)
                                      (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsDatesLiableURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def addDraftPropertyDetailsDatesLiable(id: String, propertyDetails: PropertyDetailsDatesLiable)
                                         (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$addDraftPropertyDetailsDatesLiableURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }


  def addDraftPropertyDetailsDatesInRelief(id: String, propertyDetails: PropertyDetailsDatesInRelief)
                                        (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$addDraftPropertyDetailsDatesInReliefURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def deleteDraftPropertyDetailsPeriod(id: String, propertyDetails: LocalDate)
                                           (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$deleteDraftPropertyDetailsPeriodURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsTaxAvoidance(id: String, propertyDetails: PropertyDetailsTaxAvoidance)
                                         (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsTaxAvoidanceURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def saveDraftPropertyDetailsSupportingInfo(id: String, propertyDetails: PropertyDetailsSupportingInfo)
                                          (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$saveDraftPropertyDetailsSupportingInfoURI/$id"""
    val jsonData = Json.toJson(propertyDetails)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def calculateDraftChangeLiability(id: String)
                                   (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$calculateDraftChangeLiabilityURI/$id"""
    http.GET[HttpResponse](postUrl)
  }

  def calculateDraftPropertyDetails(id: String)
                                            (implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$calculateDraftPropertyDetailsURI/$id"""
    http.GET[HttpResponse](postUrl)
  }

  def retrieveDraftPropertyDetails(id: String)(implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$retrieveDraftPropertyDetailsURI/$id"""
    http.GET[HttpResponse](postUrl)
  }

  def submitDraftPropertyDetails(id: String)(implicit authContext: StandardAuthRetrievals, headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$submitDraftPropertyDetailsURI/$id"""
    http.POST[JsValue, HttpResponse](postUrl, Json.parse("""{}"""))
  }

  def deleteDraftChargeable(id: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val deleteUrl = s"""$serviceURL$userLink/$deletePropertyDetailsURI/drafts/$id"""
    http.DELETE[HttpResponse](deleteUrl)
  }
}
