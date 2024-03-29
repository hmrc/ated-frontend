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

package controllers.subscriptionData

import config.ApplicationConfig
import controllers.auth.AuthAction
import forms.AtedForms._
import javax.inject.Inject
import play.api.Environment
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.CountryCodeUtils
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}

class RegisteredDetailsController @Inject()(mcc: MessagesControllerComponents,
                                            authAction: AuthAction,
                                            subscriptionDataService: SubscriptionDataService,
                                            serviceInfoService: ServiceInfoService,
                                            val environment: Environment,
                                            template: views.html.subcriptionData.registeredDetails)
                                           (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with CountryCodeUtils with WithUnsafeDefaultFormBinding {

  implicit val ec : ExecutionContext = mcc.executionContext

  def edit(): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        for {
          registeredDetails <- subscriptionDataService.getRegisteredDetails
        } yield {
          val populatedForm = registeredDetails match {
            case Some(x) => registeredDetailsForm.fill(x)
            case None => registeredDetailsForm
          }
          Ok(template(populatedForm, getIsoCodeTupleList, serviceInfoContent, getBackLink))
        }
      }
    }
  }

  def submit(): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        registeredDetailsForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(template(formWithErrors, getIsoCodeTupleList, serviceInfoContent, getBackLink))),
          updateDetails => {
            for {
              registeredDetails <- subscriptionDataService.updateRegisteredDetails(updateDetails)
            } yield {
              registeredDetails match {
                case Some(_) => Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view)
                case None =>
                  val errorMsg = Messages("ated.registered-details.save.error")
                  val errorForm = registeredDetailsForm.withError(key = "addressType", message = errorMsg).fill(updateDetails)
                  BadRequest(template(errorForm, getIsoCodeTupleList, serviceInfoContent, getBackLink))
              }
            }
          }
        )
      }
    }
  }

  private def getBackLink = {
    Some(controllers.subscriptionData.routes.CompanyDetailsController.view.url)
  }
}
