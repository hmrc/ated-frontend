/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AtedConnector @Inject()(appConfig: ApplicationConfig,
                              httpClient: DefaultHttpClient) extends RawResponseReads {

  val serviceURL: String = appConfig.conf.baseUrl("ated") + "/ated/"
  val http: HttpClient = httpClient

  val saveDraftReliefURI = "reliefs/save"
  val retrieveDraftReliefURI = "reliefs"
  val submitDraftReliefURI = "reliefs/submit"
  val getDetailsURI = "details"

  val retrieveFullSummaryReturns = "returns/full-summary"
  val retrievePartialSummaryReturns = "returns/partial-summary"
  val retrieveSubscriptionData = "subscription-data"
  val updateSubscriptionData = "subscription-data"
  val updateRegistrationDetailsURI = "registration-details"
  val retrieveFormBundleReturns = "returns/form-bundle"
  val retrieveLiabilityReturn = "liability-return"
  val retrievePreviousLiabilityReturn = "prev-liability-return"
  val retrieveDisposeLiability = "dispose-liability"
  val cacheDraftAddress = "update-address"
  val cacheDraftTitle = "update-title"
  val cacheDraftValue = "update-value"
  val cacheDraftPeriod = "update-period"

  val cacheDraftHasBank = "update-has-bank"
  val cacheDraftBank = "update-bank"
  val calculateDraftDisposal = "calculate"

  val cacheDraftDate = "update-date"
  val cacheDraftSelectRelief = "update-relief"
  val submit = "submit"

  def saveDraftReliefs(accountRef: String, reliefs: ReliefsTaxAvoidance)
                      (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$authLink/$baseURI/$saveDraftReliefURI"""
    val jsonData = Json.toJson(reliefs)
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }

  def retrievePeriodDraftReliefs(accountRef: String, periodKey: Int)
                                (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = authContext.atedReferenceNumber
    val getUrl = s"""$serviceURL$authLink/$baseURI/$retrieveDraftReliefURI/$periodKey"""
    http.GET[HttpResponse](getUrl)
  }

  def submitDraftReliefs(accountRef: String, periodKey: Int)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = authContext.atedReferenceNumber
    http.GET[HttpResponse]( s"""$serviceURL$authLink/$baseURI/$submitDraftReliefURI/$periodKey""")
  }

  def getDetails(identifier: String, identifierType: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = authContext.atedReferenceNumber
    http.GET[HttpResponse](s"$serviceURL$authLink/$baseURI/$getDetailsURI/$identifier/$identifierType")
  }

  def retrieveSubscriptionData()(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"""$serviceURL$userLink/$retrieveSubscriptionData"""
    http.GET[HttpResponse](getUrl)
  }

  def updateSubscriptionData(updatedSubscriptionData: UpdateSubscriptionDataRequest)
                            (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$userLink/$updateSubscriptionData"""
    http.POST[JsValue, HttpResponse](postUrl, Json.toJson(updatedSubscriptionData))
  }

  def updateRegistrationDetails(safeId: String, updateRegistrationDetails: UpdateRegistrationDetailsRequest)
                               (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$userLink/$updateRegistrationDetailsURI/$safeId"""
    http.POST[JsValue, HttpResponse](postUrl, Json.toJson(updateRegistrationDetails))
  }

  def retrieveFormBundleReturns(formBundleNumber: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"""$serviceURL$userLink/$retrieveFormBundleReturns/$formBundleNumber"""
    http.GET[HttpResponse](getUrl)
  }

  def retrieveAndCacheLiabilityReturn(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"""$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo"""
    http.GET[HttpResponse](getUrl)
  }

  def retrieveAndCachePreviousLiabilityReturn(oldFormBundleNo: String, periodKey: Int)
                                             (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"""$serviceURL$userLink/$retrievePreviousLiabilityReturn/$oldFormBundleNo/$periodKey"""
    http.GET[HttpResponse](getUrl)
  }

  def cacheDraftChangeLiabilityReturnHasBank(oldFormBundleNo: String, hasBankDetails: Boolean)
                                         (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo/$cacheDraftHasBank"""
    http.POST[JsValue, HttpResponse](postUrl, Json.toJson(hasBankDetails))
  }


  def cacheDraftChangeLiabilityReturnBank(oldFormBundleNo: String, updatedValue: BankDetails)
                                         (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo/$cacheDraftBank"""
    http.POST[JsValue, HttpResponse](postUrl, Json.toJson(updatedValue))
  }

  def submitDraftChangeLiabilityReturn(oldFormBundleNo: String)
                                      (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo/$submit"""
    http.POST[JsValue, HttpResponse](postUrl, Json.parse("""{}"""))
  }

  def retrieveAndCacheDisposeLiability(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"""$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo"""
    http.GET[HttpResponse](getUrl)
  }

  def cacheDraftDisposeLiabilityReturnDate(oldFormBundleNo: String, updatedDate: DisposeLiability)
                                          (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$cacheDraftDate"""
    http.POST[JsValue, HttpResponse](postUrl, Json.toJson(updatedDate))
  }

  def cacheDraftDisposeLiabilityReturnHasBank(oldFormBundleNo: String, hasBankDetails: Boolean)
                                            (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$cacheDraftHasBank"""
    http.POST[JsValue, HttpResponse](postUrl, Json.toJson(hasBankDetails))
  }


  def cacheDraftDisposeLiabilityReturnBank(oldFormBundleNo: String, updatedValue: BankDetails)
                                          (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$cacheDraftBank"""
    http.POST[JsValue, HttpResponse](postUrl, Json.toJson(updatedValue))
  }

  def calculateDraftDisposal(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"""$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$calculateDraftDisposal"""
    http.GET[HttpResponse](getUrl)
  }

  def submitDraftDisposeLiabilityReturn(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"""$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$submit"""
    http.POST[JsValue, HttpResponse](postUrl, Json.parse("""{}"""))
  }

  def getFullSummaryReturns(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"""$serviceURL$userLink/$retrieveFullSummaryReturns"""
    http.GET[HttpResponse](getUrl)
  }

  def getPartialSummaryReturns(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"""$serviceURL$userLink/$retrievePartialSummaryReturns"""
    http.GET[HttpResponse](getUrl)
  }

  def deleteDraftReliefs(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val deleteUrl = s"""$serviceURL$userLink/ated/$retrieveDraftReliefURI/drafts"""
    http.DELETE[HttpResponse](deleteUrl)
  }

  def deleteDraftReliefsByYear(periodKey: Int)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val deleteUrl = s"""$serviceURL$userLink/ated/$retrieveDraftReliefURI/drafts/$periodKey"""
    http.DELETE[HttpResponse](deleteUrl)
  }

}
