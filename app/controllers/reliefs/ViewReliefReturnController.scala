/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.reliefs

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import controllers.{AtedBaseController, BackLinkController}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, ReliefsService, SubscriptionDataService}

trait ViewReliefReturnController extends BackLinkController with AtedBaseController
  with ClientHelper with AuthAction {

  def reliefsService: ReliefsService
  def subscriptionDataService: SubscriptionDataService

  def viewReliefReturn(periodKey: Int, formBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        val formBundleReturnFuture = reliefsService.viewReliefReturn(periodKey, formBundleNo)
        val organisationNameFuture = subscriptionDataService.getOrganisationName
        for {
          formBundleReturn <- formBundleReturnFuture
          organisationName <- organisationNameFuture
        } yield {
          formBundleReturn match {
            case Some(x) => Ok(views.html.reliefs.viewReliefReturn(x, periodKey, formBundleNo, organisationName,
              Some(controllers.routes.PeriodSummaryController.view(periodKey).url)))
            case None => throw new RuntimeException("No reliefs found in the cache for provided period and form bundle id")
          }
        }
      }
    }
  }


  def submit(periodKey: Int, formBundleNumber: String) : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      val returnUrl = Some(routes.ViewReliefReturnController.viewReliefReturn(periodKey, formBundleNumber).url)
      ensureClientContext {
        RedirectWithBackLink(
          ChangeReliefReturnController.controllerId,
          controllers.reliefs.routes.ChangeReliefReturnController.viewChangeReliefReturn(periodKey, formBundleNumber),
          returnUrl
        )
      }
    }
  }
}

object ViewReliefReturnController extends ViewReliefReturnController {
  val delegationService: DelegationService = DelegationService
  val reliefsService: ReliefsService = ReliefsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  def subscriptionDataService: SubscriptionDataService = SubscriptionDataService
  override val controllerId = "ChangeReliefReturnController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
