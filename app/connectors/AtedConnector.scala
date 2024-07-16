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
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.StringContextOps
import scala.concurrent.{ExecutionContext, Future}

class AtedConnector @Inject()(appConfig: ApplicationConfig,
                              httpClient: HttpClientV2)
                             (implicit ec: ExecutionContext) extends RawResponseReads {

  val serviceURL: String = appConfig.conf.baseUrl("ated") + "/ated/"
  val http: HttpClientV2 = httpClient

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
    val postUrl = url"$serviceURL$authLink/$baseURI/$saveDraftReliefURI"
    val jsonData = Json.toJson(reliefs)
    //http.POST[JsValue, HttpResponse](postUrl, jsonData)
    http.post(postUrl).withBody(jsonData).execute[HttpResponse]
  }

  def retrievePeriodDraftReliefs(accountRef: String, periodKey: Int)
                                (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = authContext.atedReferenceNumber
    val getUrl = url"$serviceURL$authLink/$baseURI/$retrieveDraftReliefURI/$periodKey"
    http.get(getUrl).execute[HttpResponse]
  }

  def submitDraftReliefs(accountRef: String, periodKey: Int)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = authContext.atedReferenceNumber
    http.get( url"$serviceURL$authLink/$baseURI/$submitDraftReliefURI/$periodKey").execute[HttpResponse]
  }

  def getDetails(identifier: String, identifierType: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = authContext.atedReferenceNumber
    http.get(url"$serviceURL$authLink/$baseURI/$getDetailsURI/$identifier/$identifierType").execute[HttpResponse]
  }

  def retrieveSubscriptionData()(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = url"$serviceURL$userLink/$retrieveSubscriptionData"
    http.get(getUrl).execute[HttpResponse]
  }

  def updateSubscriptionData(updatedSubscriptionData: UpdateSubscriptionDataRequest)
                            (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = url"$serviceURL$userLink/$updateSubscriptionData"
    //http.POST[JsValue, HttpResponse](postUrl, Json.toJson(updatedSubscriptionData))
    http.post(postUrl).withBody(Json.toJson(updatedSubscriptionData)).execute[HttpResponse]
  }

  def updateRegistrationDetails(safeId: String, updateRegistrationDetails: UpdateRegistrationDetailsRequest)
                               (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = url"$serviceURL$userLink/$updateRegistrationDetailsURI/$safeId"
    //http.POST[JsValue, HttpResponse](postUrl, Json.toJson(updateRegistrationDetails))
    http.post(postUrl).withBody(Json.toJson(updateRegistrationDetails)).execute[HttpResponse]
  }

  def retrieveFormBundleReturns(formBundleNumber: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = url"$serviceURL$userLink/$retrieveFormBundleReturns/$formBundleNumber"
    http.get(getUrl).execute[HttpResponse]
  }

  def retrieveAndCacheLiabilityReturn(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = url"$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo"
    http.get(getUrl).execute[HttpResponse]
  }

  def retrieveAndCachePreviousLiabilityReturn(oldFormBundleNo: String, periodKey: Int)
                                             (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = url"$serviceURL$userLink/$retrievePreviousLiabilityReturn/$oldFormBundleNo/$periodKey"
    http.get(getUrl).execute[HttpResponse]
  }

  def cacheDraftChangeLiabilityReturnHasBank(oldFormBundleNo: String, hasBankDetails: Boolean)
                                         (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = url"$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo/$cacheDraftHasBank"
    //http.POST[JsValue, HttpResponse](postUrl, Json.toJson(hasBankDetails))
    http.post(postUrl).withBody(Json.toJson(hasBankDetails)).execute[HttpResponse]
  }


  def cacheDraftChangeLiabilityReturnBank(oldFormBundleNo: String, updatedValue: BankDetails)
                                         (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = url"$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo/$cacheDraftBank"
    //http.POST[JsValue, HttpResponse](postUrl, Json.toJson(updatedValue))
    http.post(postUrl).withBody(Json.toJson(updatedValue)).execute[HttpResponse]
  }

  def submitDraftChangeLiabilityReturn(oldFormBundleNo: String)
                                      (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = url"$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo/$submit"
    //http.POST[JsValue, HttpResponse](postUrl, Json.parse("""{}"""))
    http.post(postUrl).withBody(Json.parse("""{}""")).execute[HttpResponse]
  }

  def retrieveAndCacheDisposeLiability(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = url"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo"
    http.get(getUrl).execute[HttpResponse]
  }

  def cacheDraftDisposeLiabilityReturnDate(oldFormBundleNo: String, updatedDate: DisposeLiability)
                                          (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = url"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$cacheDraftDate"
    //http.POST[JsValue, HttpResponse](postUrl, Json.toJson(updatedDate))
    http.post(postUrl).withBody(Json.toJson(updatedDate)).execute[HttpResponse]
  }

  def cacheDraftDisposeLiabilityReturnHasBank(oldFormBundleNo: String, hasBankDetails: Boolean)
                                            (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = url"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$cacheDraftHasBank"
    //http.POST[JsValue, HttpResponse](postUrl, Json.toJson(hasBankDetails))
    http.post(postUrl).withBody(Json.toJson(hasBankDetails)).execute[HttpResponse]
  }


  def cacheDraftDisposeLiabilityReturnBank(oldFormBundleNo: String, updatedValue: BankDetails)
                                          (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = url"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$cacheDraftBank"
    //http.POST[JsValue, HttpResponse](postUrl, Json.toJson(updatedValue))
    http.post(postUrl).withBody(Json.toJson(updatedValue)).execute[HttpResponse]
  }

  def calculateDraftDisposal(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = url"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$calculateDraftDisposal"
    http.get(getUrl).execute[HttpResponse]
  }

  def submitDraftDisposeLiabilityReturn(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = url"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$submit"
    //http.POST[JsValue, HttpResponse](postUrl, Json.parse("""{}"""))
    http.post(postUrl).withBody(Json.parse("""{}""")).execute[HttpResponse]
  }

  def getFullSummaryReturns(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = url"$serviceURL$userLink/$retrieveFullSummaryReturns"
    http.get(getUrl).execute[HttpResponse]
  }

  def getPartialSummaryReturns(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = url"$serviceURL$userLink/$retrievePartialSummaryReturns"
    http.get(getUrl).execute[HttpResponse]
  }

  def deleteDraftReliefsByYear(periodKey: Int)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val deleteUrl = url"$serviceURL$userLink/ated/$retrieveDraftReliefURI/drafts/$periodKey"
    http.delete(deleteUrl).execute[HttpResponse]
  }

}
