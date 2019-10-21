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

package controllers

import config.ApplicationConfig
import controllers.auth.AuthAction
import javax.inject.Inject
import models.StandardAuthRetrievals
import play.api.Logger
import play.api.mvc._
import services.DelegationService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.SessionUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class HomeController @Inject()(mcc: MessagesControllerComponents,
                               authAction: AuthAction,
                               applicationConfig: ApplicationConfig)
  extends FrontendController(mcc) {

  implicit val ec : ExecutionContext = mcc.executionContext

  def home(callerId: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedForNoEnrolments { implicit authContext =>
      Future.successful {
        if (isSubscribedUser) redirectSubscribedUser(callerId)
        else Redirect(applicationConfig.subscriptionStartPage)
      }
    }
  }

  private def isSubscribedUser(implicit authContext: StandardAuthRetrievals): Boolean = {
    authContext.agentRefNo.isDefined || Try{authContext.atedReferenceNumber}.isSuccess
  }

  private def redirectSubscribedUser(callerId: Option[String])(implicit authContext: StandardAuthRetrievals, request: Request[AnyContent]): Result = {
    if (authContext.isAgent) {
      Logger.debug("[redirectSubscribedUser] agent redirected to mandate:" + authContext)
      Redirect(applicationConfig.agentRedirectedToMandate)
    }
    else {
      Logger.debug("[redirectSubscribedUser] user redirected to account summary:" + authContext)
      callerId match {
        case Some(x) => Redirect(controllers.routes.AccountSummaryController.view()).addingToSession(SessionUtils.sessionCallerId -> x)
        case None => Redirect(controllers.routes.AccountSummaryController.view())
      }
    }
  }

}
