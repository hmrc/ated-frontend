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

package controllers.reliefs

import config.ApplicationConfig
import connectors.{BackLinkCacheService, DataCacheService}
import controllers.BackLinkService
import controllers.auth.{AuthAction, ClientHelper}
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReliefsService, ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

class ViewReliefReturnController @Inject()(mcc: MessagesControllerComponents,
                                           authAction: AuthAction,
                                           subscriptionDataService: SubscriptionDataService,
                                           changeReliefReturnController: ChangeReliefReturnController,
                                           serviceInfoService: ServiceInfoService,
                                           val reliefsService: ReliefsService,
                                           val dataCacheService: DataCacheService,
                                           val backLinkCacheService: BackLinkCacheService,
                                           template: views.html.reliefs.viewReliefReturn)
                                          (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with BackLinkService with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId = "ChangeReliefReturnController"

  def viewReliefReturn(periodKey: Int, formBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        val formBundleReturnFuture = reliefsService.viewReliefReturn(periodKey, formBundleNo)
        val organisationNameFuture = subscriptionDataService.getOrganisationName
        for {
          (formBundleReturn, isEditable) <- formBundleReturnFuture
          serviceInfoContent <- serviceInfoService.getPartial
          organisationName <- organisationNameFuture
        } yield {
          formBundleReturn match {
            case Some(x) => Ok(template(x, periodKey, formBundleNo, organisationName, isEditable, serviceInfoContent,
              Some(controllers.routes.PeriodSummaryController.view(periodKey).url)))
            case None => throw new RuntimeException("No reliefs found in the cache for provided period and form bundle id")
          }
        }
      }
    }
  }


  def submit(periodKey: Int, formBundleNumber: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      val returnUrl = Some(routes.ViewReliefReturnController.viewReliefReturn(periodKey, formBundleNumber).url)
      ensureClientContext {
        redirectWithBackLink(
          changeReliefReturnController.controllerId,
          controllers.reliefs.routes.ChangeReliefReturnController.viewChangeReliefReturn(periodKey, formBundleNumber),
          returnUrl
        )
      }
    }
  }
}
