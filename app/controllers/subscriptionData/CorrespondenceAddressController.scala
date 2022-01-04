/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.{Environment, Logging}
import services.{ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AtedUtils, CountryCodeUtils}

import scala.concurrent.{ExecutionContext, Future}

class CorrespondenceAddressController @Inject()(mcc: MessagesControllerComponents,
                                                authAction: AuthAction,
                                                subscriptionDataService: SubscriptionDataService,
                                                serviceInfoService: ServiceInfoService,
                                                val environment: Environment,
                                                template: views.html.subcriptionData.correspondenceAddress,
                                                templateError: views.html.global_error)
                                               (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with CountryCodeUtils with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def editAddress: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        val correspondenceAddressResponse = subscriptionDataService.getCorrespondenceAddress
        for {
          correspondenceAddress <- correspondenceAddressResponse
        } yield {
          val populatedForm = correspondenceAddress match {
            case Some(x) => correspondenceAddressForm.fill(x.addressDetails)
            case None => correspondenceAddressForm
          }
          Ok(template(populatedForm, getIsoCodeTupleList, serviceInfoContent, getBackLink))
        }
      }
    }
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        AtedForms.verifyUKPostCode(correspondenceAddressForm.bindFromRequest).fold(
          formWithErrors => Future.successful(BadRequest(template(formWithErrors,
            getIsoCodeTupleList, serviceInfoContent, getBackLink))),
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
                    logger.warn(s"[CorrespondenceAddressController][submit] - Unable to update address")
                    Ok(templateError("ated.generic.error.title", "ated.generic.error.header",
                      "ated.generic.error.message", Some("ated.generic.error.message2"), None, None, None, serviceInfoContent, appConfig))
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
