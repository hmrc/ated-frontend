/*
 * Copyright 2019 HM Revenue & Customs
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
import services.{DelegationService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.CountryCodeUtils

import scala.concurrent.{ExecutionContext, Future}


class OverseasCompanyRegistrationController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      subscriptionDataService: SubscriptionDataService,
                                                      val environment: Environment)
                                                     (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with CountryCodeUtils {

  implicit val ec : ExecutionContext = mcc.executionContext

  def edit: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        overseasCompanyRegistration <- subscriptionDataService.getOverseasCompanyRegistration
      } yield {
        val result = OverseasCompanyRegistration(overseasCompanyRegistration
          .map(_.idNumber), overseasCompanyRegistration.map(_.issuingInstitution), overseasCompanyRegistration.map(_.issuingCountryCode))
        Ok(views.html.subcriptionData.overseasCompanyRegistration
        (overseasCompanyRegistrationForm.fill(result), getIsoCodeTupleList, getBackLink()))
      }
    }
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      overseasCompanyRegistrationForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.subcriptionData.overseasCompanyRegistration
          (formWithErrors, getIsoCodeTupleList, getBackLink()))),
        data => {
          for {
            _ <- subscriptionDataService.updateOverseasCompanyRegistration(Identification(data.businessUniqueId
              .getOrElse(""),data.issuingInstitution.getOrElse(""), data.countryCode.getOrElse("")))
          } yield {
            Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
          }
        }
      )
    }
  }

  private def getBackLink(): Some[String] = {
    Some(routes.CompanyDetailsController.view().url)
  }
}
