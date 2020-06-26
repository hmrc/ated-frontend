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
import models.EditContactDetails
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class EditContactDetailsController @Inject()(mcc: MessagesControllerComponents,
                                             authAction: AuthAction,
                                             serviceInfoService: ServiceInfoService,
                                             subscriptionDataService: SubscriptionDataService)
                                            (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) {

implicit val ec: ExecutionContext = mcc.executionContext

  def edit: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        for {
          contactAddress <- subscriptionDataService.getCorrespondenceAddress

        } yield {
          val populatedForm = contactAddress.fold(editContactDetailsForm) { x =>
            val editContactDetails = EditContactDetails(firstName = x.name1.getOrElse(""),
              lastName = x.name2.getOrElse(""),
              phoneNumber = x.contactDetails.fold("")(a => a.phoneNumber.getOrElse("")))
            editContactDetailsForm.fill(editContactDetails)
          }
          Ok(views.html.subcriptionData.editContactDetails(populatedForm, serviceInfoContent, getBackLink))
        }
      }
    }
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        editContactDetailsForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.subcriptionData.editContactDetails(formWithErrors, serviceInfoContent, getBackLink))),
          editedClientData => {
            for {
              editedContact <- subscriptionDataService.editContactDetails(editedClientData)
            } yield {
              editedContact match {
                case Some(_) => Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
                case None =>
                  val errorMsg = Messages("ated.contact-details.error.general.addressType")
                  val errorForm = editContactDetailsForm.withError(key = "addressType", message = errorMsg).fill(editedClientData)
                  BadRequest(views.html.subcriptionData.editContactDetails(errorForm, serviceInfoContent, getBackLink))
              }
            }
          }
        )
      }
    }
  }

  private def getBackLink = {
    Some(controllers.subscriptionData.routes.CompanyDetailsController.view().url)
  }
}
