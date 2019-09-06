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

import controllers.auth.{AuthAction, ExternalUrls}
import models.StandardAuthRetrievals
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.DelegationService
import utils.SessionUtils

import scala.concurrent.Future

trait HomeController extends AtedBaseController with AuthAction {

  def home(callerId: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authorisedForNoEnrolments { implicit authContext =>
      Future.successful {
        if (isSubscribedUser) redirectSubscribedUser(callerId)
        else Redirect(ExternalUrls.subscriptionStartPage)
      }
    }
  }

  private def isSubscribedUser(implicit authContext: StandardAuthRetrievals): Boolean = {
  authContext.agentRefNo.isDefined || authContext.atedReferenceNumber.isDefined
  }

  private def redirectSubscribedUser(callerId: Option[String])(implicit authContext: StandardAuthRetrievals, request: Request[AnyContent]): Result = {
    if (authContext.isAgent) {
      Logger.debug("agent redirected to mandate:" + StandardAuthRetrievals)
      Redirect(ExternalUrls.agentRedirectedToMandate)
    }
    else {
      Logger.debug("user redirected to account summary:" + StandardAuthRetrievals)
      callerId match {
        case Some(x) => Redirect(controllers.routes.AccountSummaryController.view()).addingToSession(SessionUtils.sessionCallerId -> x)
        case None => Redirect(controllers.routes.AccountSummaryController.view())
      }
    }
  }

}

object HomeController extends HomeController {
  val delegationService: DelegationService = DelegationService
}
