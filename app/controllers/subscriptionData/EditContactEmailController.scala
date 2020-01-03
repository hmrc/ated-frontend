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

package controllers.subscriptionData

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.AuthAction
import forms.AtedForms._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DelegationService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class EditContactEmailController @Inject()(mcc: MessagesControllerComponents,
                                           authAction: AuthAction,
                                           subscriptionDataService: SubscriptionDataService)
                                          (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) {

  implicit val ec: ExecutionContext = mcc.executionContext

  def edit : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        emailWithConsent <- subscriptionDataService.getEmailWithConsent
      } yield {
        val populatedForm = emailWithConsent match {
          case Some(x) => editContactDetailsEmailForm.fill(x)
          case _ => editContactDetailsEmailForm
        }
        Ok(views.html.subcriptionData.editContactEmail(populatedForm, getBackLink))
      }
    }
  }


  def submit : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>

      validateEmail(editContactDetailsEmailForm.bindFromRequest).fold(
        formWithErrors => Future.successful(BadRequest(views.html.subcriptionData.editContactEmail(formWithErrors, getBackLink))),
        editedClientData => {
          for {
            _ <- subscriptionDataService.editEmailWithConsent(editedClientData)
          } yield {
            Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
          }
        }
      )
    }
  }

  private def getBackLink = {
    Some(controllers.subscriptionData.routes.CompanyDetailsController.view().url)
  }
}
