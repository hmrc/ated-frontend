/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import config.FrontendDelegationConnector
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime}
import services.{SubscriptionDataService, FormBundleReturnsService, SummaryReturnsService}
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.PeriodUtils

trait FormBundleReturnController extends AtedBaseController with AtedFrontendAuthHelpers with DelegationAwareActions {

  def formBundleReturnsService: FormBundleReturnsService

  def summaryReturnsService: SummaryReturnsService
  def subscriptionDataService: SubscriptionDataService

  def view(formBundleNumber: String, periodKey: Int) = AuthAction(AtedRegime) {
      implicit atedContext =>
        for {
          formBundleReturn <- formBundleReturnsService.getFormBundleReturns(formBundleNumber)
          periodSummaries <- summaryReturnsService.getPeriodSummaryReturns(periodKey)
          organisationName <- subscriptionDataService.getOrganisationName
        } yield {
          val valuesToDisplay = formBundleReturn.map(x => PeriodUtils.getOrderedReturnPeriodValues(x.lineItem, x.dateOfAcquisition)).getOrElse(Nil)
          val periodsToDisplay = formBundleReturn.map(x => PeriodUtils.getDisplayFormBundleProperties(x.lineItem)).getOrElse(Nil)

          val formBundlePeriodReturn = periodSummaries.flatMap{
            period =>
              val returns = List(period.submittedReturns.map(_.currentLiabilityReturns), period.submittedReturns.map(_.oldLiabilityReturns))
              val mergedReturns = returns.flatten.flatten
              mergedReturns.find(_.formBundleNo == formBundleNumber)
          }

          val changeAllowed = formBundlePeriodReturn.map(_.changeAllowed).getOrElse(false)
          val editAllowed =  valuesToDisplay.size <= 1

          Ok(views.html.formBundleReturn(periodKey, formBundleReturn, formBundleNumber, organisationName, changeAllowed, editAllowed,
            valuesToDisplay,
            periodsToDisplay,
            getBackLink(periodKey)))
        }
    }

  private def getBackLink(periodKey: Int) = {
    Some(routes.PeriodSummaryController.view(periodKey).url)
  }
}

object FormBundleReturnController extends FormBundleReturnController {
  val delegationConnector = FrontendDelegationConnector
  val formBundleReturnsService = FormBundleReturnsService
  val summaryReturnsService = SummaryReturnsService
  val subscriptionDataService = SubscriptionDataService
}
