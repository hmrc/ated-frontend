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
    val postUrl = s"$serviceURL$authLink/$baseURI/$saveDraftReliefURI"
    val jsonData = Json.toJson(reliefs)
    http.post(url"$postUrl").withBody(jsonData).execute[HttpResponse]
  }

  def retrievePeriodDraftReliefs(accountRef: String, periodKey: Int)
                                (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = authContext.atedReferenceNumber
    val getUrl = s"$serviceURL$authLink/$baseURI/$retrieveDraftReliefURI/$periodKey"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def submitDraftReliefs(accountRef: String, periodKey: Int)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = authContext.atedReferenceNumber
    val getUrl = s"$serviceURL$authLink/$baseURI/$submitDraftReliefURI/$periodKey"
    http.get( url"$getUrl").execute[HttpResponse]
  }

  def getDetails(identifier: String, identifierType: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val baseURI = "ated"
    val authLink = authContext.atedReferenceNumber

    val getUrl = s"$serviceURL$authLink/$baseURI/$getDetailsURI/$identifier/$identifierType"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def retrieveSubscriptionData()(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"$serviceURL$userLink/$retrieveSubscriptionData"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def updateSubscriptionData(updatedSubscriptionData: UpdateSubscriptionDataRequest)
                            (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"$serviceURL$userLink/$updateSubscriptionData"
    http.post(url"$postUrl").withBody(Json.toJson(updatedSubscriptionData)).execute[HttpResponse]
  }

  def updateRegistrationDetails(safeId: String, updateRegistrationDetails: UpdateRegistrationDetailsRequest)
                               (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"$serviceURL$userLink/$updateRegistrationDetailsURI/$safeId"
    http.post(url"$postUrl").withBody(Json.toJson(updateRegistrationDetails)).execute[HttpResponse]
  }

  def retrieveFormBundleReturns(formBundleNumber: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"$serviceURL$userLink/$retrieveFormBundleReturns/$formBundleNumber"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def retrieveAndCacheLiabilityReturn(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def retrieveAndCachePreviousLiabilityReturn(oldFormBundleNo: String, periodKey: Int)
                                             (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"$serviceURL$userLink/$retrievePreviousLiabilityReturn/$oldFormBundleNo/$periodKey"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def cacheDraftChangeLiabilityReturnHasBank(oldFormBundleNo: String, hasBankDetails: Boolean)
                                         (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo/$cacheDraftHasBank"
    http.post(url"$postUrl").withBody(Json.toJson(hasBankDetails)).execute[HttpResponse]
  }


  def cacheDraftChangeLiabilityReturnBank(oldFormBundleNo: String, updatedValue: BankDetails)
                                         (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo/$cacheDraftBank"
    http.post(url"$postUrl").withBody(Json.toJson(updatedValue)).execute[HttpResponse]
  }

  def submitDraftChangeLiabilityReturn(oldFormBundleNo: String)
                                      (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"$serviceURL$userLink/$retrieveLiabilityReturn/$oldFormBundleNo/$submit"
    http.post(url"$postUrl").withBody(Json.parse("""{}""")).execute[HttpResponse]
  }

  def retrieveAndCacheDisposeLiability(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def cacheDraftDisposeLiabilityReturnDate(oldFormBundleNo: String, updatedDate: DisposeLiability)
                                          (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$cacheDraftDate"
    http.post(url"$postUrl").withBody(Json.toJson(updatedDate)).execute[HttpResponse]
  }

  def cacheDraftDisposeLiabilityReturnHasBank(oldFormBundleNo: String, hasBankDetails: Boolean)
                                            (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$cacheDraftHasBank"
    http.post(url"$postUrl").withBody(Json.toJson(hasBankDetails)).execute[HttpResponse]
  }

  def cacheDraftDisposeLiabilityReturnBank(oldFormBundleNo: String, updatedValue: BankDetails)
                                          (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$cacheDraftBank"
    http.post(url"$postUrl").withBody(Json.toJson(updatedValue)).execute[HttpResponse]
  }

  def calculateDraftDisposal(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$calculateDraftDisposal"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def submitDraftDisposeLiabilityReturn(oldFormBundleNo: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val postUrl = s"$serviceURL$userLink/$retrieveDisposeLiability/$oldFormBundleNo/$submit"
    http.post(url"$postUrl").withBody(Json.parse("""{}""")).execute[HttpResponse]
  }

  def getFullSummaryReturns(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"$serviceURL$userLink/$retrieveFullSummaryReturns"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def getPartialSummaryReturns(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val getUrl = s"$serviceURL$userLink/$retrievePartialSummaryReturns"
    http.get(url"$getUrl").execute[HttpResponse]
  }

  def deleteDraftReliefsByYear(periodKey: Int)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier): Future[HttpResponse] = {
    val userLink = authContext.atedReferenceNumber
    val deleteUrl = s"$serviceURL$userLink/ated/$retrieveDraftReliefURI/drafts/$periodKey"
    http.delete(url"$deleteUrl").execute[HttpResponse]
  }

}
