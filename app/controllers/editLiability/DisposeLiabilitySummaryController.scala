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

package controllers.editLiability

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.{AtedBaseController, BackLinkController}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, ClientHelper}
import services.{DisposeLiabilityReturnService, SubscriptionDataService}
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait DisposeLiabilitySummaryController extends BackLinkController
  with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  def disposeLiabilityReturnService: DisposeLiabilityReturnService

  def subscriptionDataService: SubscriptionDataService

  def view(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo) flatMap {
          case Some(x) =>
            currentBackLink.map { backLink =>
              Ok(views.html.editLiability.disposeLiabilitySummary(x, backLink))
            }
          case None => Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
        }
      }

  }

  def submit(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        RedirectWithBackLink(
          DisposeLiabilityDeclarationController.controllerId,
          controllers.editLiability.routes.DisposeLiabilityDeclarationController.view(oldFormBundleNo),
          Some(controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo).url)
        )
      }
  }

  def viewPrintFriendlyDisposeLiabilityReturn(oldFormBundleNo: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        for {
          retrieveLiabilityReturn <- disposeLiabilityReturnService.retrieveLiabilityReturn(oldFormBundleNo)
          organisationName <- subscriptionDataService.getOrganisationName
        }
          yield {
            retrieveLiabilityReturn match {
              case Some(x) => Ok(views.html.editLiability.disposeLiabilityPrintFriendly(x, organisationName))
              case None => Redirect(controllers.routes.AccountSummaryController.view())
            }
          }
      }
  }

}

object DisposeLiabilitySummaryController extends DisposeLiabilitySummaryController {
  val delegationConnector = FrontendDelegationConnector
  val disposeLiabilityReturnService = DisposeLiabilityReturnService
  val dataCacheConnector = DataCacheConnector
  val subscriptionDataService = SubscriptionDataService
  override val controllerId = "DisposeLiabilitySummaryController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
