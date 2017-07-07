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

import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime, AtedSubscriptionNotNeededRegime, ExternalUrls}
import models.AtedContext
import play.api.Logger
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.play.config.RunMode
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait HomeController extends AtedBaseController with AtedFrontendAuthHelpers with RunMode {

  def home = AuthAction(AtedSubscriptionNotNeededRegime) {
    implicit atedContext =>
      Future.successful {
        if (isSubscribedUser) redirectSubscribedUser
        else Redirect(ExternalUrls.subscriptionStartPage)
      }
  }

  def summary = AuthAction(AtedRegime) {
    implicit atedContext =>
      Future.successful(redirectSubscribedUser)
  }

  private def isSubscribedUser(implicit atedContext: AtedContext): Boolean = {
    val user = atedContext.user.authContext
    user.principal.accounts.agent.flatMap(_.agentBusinessUtr).isDefined || user.principal.accounts.ated.isDefined
  }

  private def redirectSubscribedUser(implicit atedContext: AtedContext): Result = {
    if (atedContext.user.isAgent) {
      Logger.debug("agent redirected to mandate:" + atedContext)
      Redirect(ExternalUrls.agentRedirectedToMandate)
    }
    else {
      Logger.debug("user redirected to account summary:" + atedContext)
      Redirect(controllers.routes.AccountSummaryController.view())
    }
  }

}


object HomeController extends HomeController
