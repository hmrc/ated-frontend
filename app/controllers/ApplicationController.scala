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

package controllers

import config.ApplicationConfig
import controllers.auth.AuthAction
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ApplicationController @Inject()(mcc: MessagesControllerComponents,
                                      authAction: AuthAction,
                                      template: views.html.unauthorised,
                                      individual: views.html.error.individual)
                                     (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) {

  implicit val ec: ExecutionContext = mcc.executionContext

  def unauthorised(isSa: Boolean): Action[AnyContent] = Action.async { implicit request =>
    if(isSa) {
      Future.successful(Ok(template()(implicitly, implicitly)))
    } else {
      Future.successful(Ok(individual()(implicitly, implicitly, implicitly)))
    }
  }

  def cancel: Action[AnyContent] = Action {
    val serviceRedirectUrl: String = Try{appConfig.conf.getString("cancelRedirectUrl")}.getOrElse("https://www.gov.uk/")
    Redirect(serviceRedirectUrl)
  }

  def logout: Action[AnyContent] = Action {
    Redirect(appConfig.serviceSignOut).withNewSession
  }

  def keepAlive: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedForNoEnrolments { _ =>
      Future.successful(Ok("OK"))
    }
  }

  def redirectToGuidance: Action[AnyContent] = Action {
    Redirect(appConfig.signOutRedirect).withNewSession
  }

  def redirectToSignIn: Action[AnyContent] = Action {
    Redirect(appConfig.signIn).withNewSession
  }

}
