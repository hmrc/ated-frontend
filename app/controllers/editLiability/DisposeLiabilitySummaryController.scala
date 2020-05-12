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

package controllers.editLiability

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.BackLinkController
import controllers.auth.{AuthAction, ClientHelper}
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DisposeLiabilityReturnService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class DisposeLiabilitySummaryController @Inject()(mcc: MessagesControllerComponents,
                                                  disposeLiabilityReturnService: DisposeLiabilityReturnService,
                                                  subscriptionDataService: SubscriptionDataService,
                                                  authAction: AuthAction,
                                                  disposeLiabilityDeclarationController: DisposeLiabilityDeclarationController,
                                                  val dataCacheConnector: DataCacheConnector,
                                                  val backLinkCacheConnector: BackLinkCacheConnector)
                                                 (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with BackLinkController with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = "DisposeLiabilitySummaryController"

  def view(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
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
  }

  def submit(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        redirectWithBackLink(
          disposeLiabilityDeclarationController.controllerId,
          controllers.editLiability.routes.DisposeLiabilityDeclarationController.view(oldFormBundleNo),
          Some(controllers.editLiability.routes.DisposeLiabilitySummaryController.view(oldFormBundleNo).url)
        )
      }
    }
  }

  def viewPrintFriendlyDisposeLiabilityReturn(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
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

}
