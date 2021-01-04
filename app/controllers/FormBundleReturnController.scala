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

package controllers

import config.ApplicationConfig
import controllers.auth.AuthAction
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{FormBundleReturnsService, ServiceInfoService, SubscriptionDataService, SummaryReturnsService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.PeriodUtils

import scala.concurrent.ExecutionContext

class FormBundleReturnController @Inject()(mcc: MessagesControllerComponents,
                                           authAction: AuthAction,
                                           formBundleReturnsService: FormBundleReturnsService,
                                           summaryReturnsService: SummaryReturnsService,
                                           serviceInfoService: ServiceInfoService,
                                           subscriptionDataService: SubscriptionDataService,
                                           template: views.html.formBundleReturn)
                                          (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) {

  implicit val ec : ExecutionContext = mcc.executionContext

  def view(formBundleNumber: String, periodKey: Int): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        formBundleReturn <- formBundleReturnsService.getFormBundleReturns(formBundleNumber)
        periodSummaries <- summaryReturnsService.getPeriodSummaryReturns(periodKey)
        organisationName <- subscriptionDataService.getOrganisationName
        serviceInfoContent <- serviceInfoService.getPartial
      } yield {
        val valuesToDisplay = formBundleReturn.map(x => PeriodUtils.getOrderedReturnPeriodValues(x.lineItem, x.dateOfAcquisition)).getOrElse(Nil)
        val periodsToDisplay = formBundleReturn.map(x => PeriodUtils.getDisplayFormBundleProperties(x.lineItem)).getOrElse(Nil)

        val formBundlePeriodReturn = periodSummaries.flatMap {
          period =>
            val returns = List(period.submittedReturns.map(_.currentLiabilityReturns), period.submittedReturns.map(_.oldLiabilityReturns))
            val mergedReturns = returns.flatten.flatten
            mergedReturns.find(_.formBundleNo == formBundleNumber)
        }

        val changeAllowed = formBundlePeriodReturn.exists(_.changeAllowed)
        val editAllowed = valuesToDisplay.size <= 1

        Ok(template(periodKey, formBundleReturn, formBundleNumber, organisationName, changeAllowed, editAllowed,
          valuesToDisplay,
          periodsToDisplay,
          serviceInfoContent,
          getBackLink(periodKey)))
        }
      }
    }

  private def getBackLink(periodKey: Int) = {
    Some(routes.PeriodSummaryController.view(periodKey).url)
  }
}
