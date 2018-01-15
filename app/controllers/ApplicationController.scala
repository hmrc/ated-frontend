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

package controllers

import controllers.auth.{ExternalUrls, AtedSubscriptionNotNeededRegime, UnauthorisedRegime, AtedFrontendAuthHelpers}
import play.api.Play
import play.api.mvc.Action
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait ApplicationController extends AtedFrontendAuthHelpers with RunMode {

  import play.api.Play.current

  def unauthorised = AuthAction(UnauthorisedRegime) { implicit atedContext =>
    Future.successful(Ok(views.html.unauthorised()))
  }

  def cancel = Action { implicit request =>
    val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"cancelRedirectUrl")
    Redirect(serviceRedirectUrl.getOrElse("https://www.gov.uk/"))
  }

  def logout = UnauthorisedAction { implicit request =>
    Redirect(controllers.routes.QuestionnaireController.showQuestionnaire).withNewSession
  }

  def keepAlive = AuthAction(AtedSubscriptionNotNeededRegime) {
    implicit atedContext =>
      Future.successful(Ok("OK"))
  }
}

object ApplicationController extends ApplicationController
