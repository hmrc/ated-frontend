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

import controllers.AtedBaseController
import controllers.auth.AuthAction
import forms.OverseasCompanyRegistrationForm._
import models.{Identification, OverseasCompanyRegistration}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, SubscriptionDataService}
import utils.CountryCodeUtils

import scala.concurrent.Future

object OverseasCompanyRegistrationController extends OverseasCompanyRegistrationController {
  val subscriptionDataService: SubscriptionDataService = SubscriptionDataService
  val delegationService: DelegationService = DelegationService
}


trait OverseasCompanyRegistrationController extends AtedBaseController with AuthAction {

  def subscriptionDataService: SubscriptionDataService

  def edit: Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      for {
        overseasCompanyRegistration <- subscriptionDataService.getOverseasCompanyRegistration
      } yield {
        val result = OverseasCompanyRegistration(overseasCompanyRegistration.map(_.idNumber), overseasCompanyRegistration.map(_.issuingInstitution), overseasCompanyRegistration.map(_.issuingCountryCode))
        Ok(views.html.subcriptionData.overseasCompanyRegistration
        (overseasCompanyRegistrationForm.fill(result), CountryCodeUtils.getIsoCodeTupleList, getBackLink()))
      }
    }
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      overseasCompanyRegistrationForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.subcriptionData.overseasCompanyRegistration
          (formWithErrors, CountryCodeUtils.getIsoCodeTupleList, getBackLink()))),
        data => {
          for {
            _ <- subscriptionDataService.updateOverseasCompanyRegistration(Identification(data.businessUniqueId.getOrElse(""),data.issuingInstitution.getOrElse(""), data.countryCode.getOrElse("")))
          } yield {
            Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
          }
        }
      )
    }
  }

  private def getBackLink() = {
    Some(routes.CompanyDetailsController.view().url)
  }
}
