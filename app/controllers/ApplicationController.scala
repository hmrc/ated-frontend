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

package controllers

import config.ApplicationConfig
import controllers.auth.AuthAction
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ApplicationController @Inject()(mcc: MessagesControllerComponents,
                                      authAction: AuthAction)
                                     (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) {

  implicit val ec: ExecutionContext = mcc.executionContext

  def unauthorised(isSa: Boolean): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.unauthorised()(isSa, implicitly, implicitly, implicitly)))
  }

  def cancel: Action[AnyContent] = Action { implicit request =>
      val serviceRedirectUrl: String = Try{appConfig.conf.getString("cancelRedirectUrl")}.getOrElse("https://www.gov.uk/")
      Redirect(serviceRedirectUrl)
    }

    def logout: Action[AnyContent] = Action { implicit request =>
      Redirect(appConfig.serviceSignOut).withNewSession
    }

    def keepAlive: Action[AnyContent] = Action.async { implicit request =>
      authAction.authorisedForNoEnrolments { implicit authContext =>
        Future.successful(Ok("OK"))
      }
    }
  }
