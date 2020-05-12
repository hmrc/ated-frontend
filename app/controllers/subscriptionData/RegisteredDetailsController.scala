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
import controllers.auth.AuthAction
import forms.AtedForms._
import javax.inject.Inject
import play.api.Environment
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubscriptionDataService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.CountryCodeUtils

import scala.concurrent.{ExecutionContext, Future}

class RegisteredDetailsController @Inject()(mcc: MessagesControllerComponents,
                                            authAction: AuthAction,
                                            subscriptionDataService: SubscriptionDataService,
                                            val environment: Environment)
                                           (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with CountryCodeUtils {

  implicit val ec : ExecutionContext = mcc.executionContext

  def edit(): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        registeredDetails <- subscriptionDataService.getRegisteredDetails
      } yield {
        val populatedForm = registeredDetails match {
          case Some(x) => registeredDetailsForm.fill(x)
          case None => registeredDetailsForm
        }
        Ok(views.html.subcriptionData.registeredDetails(populatedForm, getIsoCodeTupleList, getBackLink))
      }
    }
  }

  def submit(): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      registeredDetailsForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.subcriptionData
          .registeredDetails(formWithErrors, getIsoCodeTupleList, getBackLink))),
        updateDetails => {
          for {
            registeredDetails <- subscriptionDataService.updateRegisteredDetails(updateDetails)
          } yield {
            registeredDetails match {
              case Some(_) => Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
              case None =>
                val errorMsg = Messages("ated.registered-details.save.error")
                val errorForm = registeredDetailsForm.withError(key = "addressType", message = errorMsg).fill(updateDetails)
                BadRequest(views.html.subcriptionData.registeredDetails(errorForm, getIsoCodeTupleList, getBackLink))
            }
          }
        }
      )
    }
  }

  private def getBackLink = {
    Some(controllers.subscriptionData.routes.CompanyDetailsController.view().url)
  }
}
