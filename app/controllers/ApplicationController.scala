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
import play.api.Play
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.DelegationService
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction

import scala.concurrent.Future

trait ApplicationController extends AuthAction {

  import play.api.Play.current

  def unauthorised(isSa: Boolean) : Action[AnyContent] = Action.async { implicit request =>
      Future.successful(Ok(views.html.unauthorised()(isSa, implicitly, implicitly)))
  }

    def cancel: Action[AnyContent] = Action { implicit request =>
      val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"cancelRedirectUrl")
      Redirect(serviceRedirectUrl.getOrElse("https://www.gov.uk/"))
    }

    def logout = UnauthorisedAction { implicit request =>
      Redirect(ApplicationConfig.serviceSignOut).withNewSession
    }

    def keepAlive: Action[AnyContent] = Action.async { implicit request =>
      authorisedForNoEnrolments { implicit authContext =>
        Future.successful(Ok("OK"))
      }
    }
  }

object ApplicationController extends ApplicationController {
  val delegationService: DelegationService = DelegationService
}
