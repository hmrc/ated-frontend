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
import forms.AtedForms
import forms.AtedForms._
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Environment, Logger}
import services.SubscriptionDataService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{AtedUtils, CountryCodeUtils}

import scala.concurrent.{ExecutionContext, Future}

class CorrespondenceAddressController @Inject()(mcc: MessagesControllerComponents,
                                                authAction: AuthAction,
                                                subscriptionDataService: SubscriptionDataService,
                                                val environment: Environment)
                                               (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with CountryCodeUtils {

  implicit val ec: ExecutionContext = mcc.executionContext

  def editAddress: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      val correspondenceAddressResponse = subscriptionDataService.getCorrespondenceAddress
      for {
        correspondenceAddress <- correspondenceAddressResponse
      } yield {
        val populatedForm = correspondenceAddress match {
          case Some(x) => correspondenceAddressForm.fill(x.addressDetails)
          case None => correspondenceAddressForm
        }
        Ok(views.html.subcriptionData.correspondenceAddress(populatedForm, getIsoCodeTupleList, getBackLink))
      }
    }
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      AtedForms.verifyUKPostCode(correspondenceAddressForm.bindFromRequest).fold(
        formWithErrors => Future.successful(BadRequest(views.html.subcriptionData.correspondenceAddress(formWithErrors,
          getIsoCodeTupleList, getBackLink))),
        addressData => {
          val trimmedPostCode = AtedUtils.formatPostCode(addressData.postalCode)
          val trimmedAddress = addressData.copy(postalCode = trimmedPostCode)
          for {
            correspondenceAddress <- subscriptionDataService.updateCorrespondenceAddressDetails(trimmedAddress)
          }
            yield {
              correspondenceAddress match {
                case Some(_) => Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
                case None =>
                  Logger.warn(s"[CorrespondenceAddressController][submit] - Unable to update address")
                  Ok(views.html.global_error("ated.generic.error.title", "ated.generic.error.header", "ated.generic.error.message", None, None, None, appConfig))
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
