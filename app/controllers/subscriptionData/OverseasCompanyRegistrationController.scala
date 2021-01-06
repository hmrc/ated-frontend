/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.OverseasCompanyRegistrationForm._
import javax.inject.Inject
import models.{Identification, OverseasCompanyRegistration}
import play.api.Environment
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.CountryCodeUtils

import scala.concurrent.{ExecutionContext, Future}


class OverseasCompanyRegistrationController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      subscriptionDataService: SubscriptionDataService,
                                                      serviceInfoService: ServiceInfoService,
                                                      val environment: Environment,
                                                      template: views.html.subcriptionData.overseasCompanyRegistration)
                                                     (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with CountryCodeUtils {

  implicit val ec : ExecutionContext = mcc.executionContext

  def edit: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        overseasCompanyRegistration <- subscriptionDataService.getOverseasCompanyRegistration
        serviceInfoContent <- serviceInfoService.getPartial
      } yield {
        val result = OverseasCompanyRegistration(overseasCompanyRegistration
          .map(_.idNumber), overseasCompanyRegistration.map(_.issuingInstitution), overseasCompanyRegistration.map(_.issuingCountryCode))
        Ok(template
        (overseasCompanyRegistrationForm.fill(result), getIsoCodeTupleList, serviceInfoContent, getBackLink()))
      }
    }
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.flatMap { serviceInfoContent =>
        overseasCompanyRegistrationForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(template
            (formWithErrors, getIsoCodeTupleList, serviceInfoContent, getBackLink()))),
          data => {
            for {
              _ <- subscriptionDataService.updateOverseasCompanyRegistration(Identification(data.businessUniqueId
                .getOrElse(""), data.issuingInstitution.getOrElse(""), data.countryCode.getOrElse("")))
            } yield {
              Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
            }
          }
        )
      }
    }
  }

  private def getBackLink(): Some[String] = {
    Some(routes.CompanyDetailsController.view().url)
  }
}
