/*
 * Copyright 2017 HM Revenue & Customs
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

import config.FrontendDelegationConnector
import controllers.{AtedBaseController, routes}
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime}
import services.SubscriptionDataService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import utils.CountryCodeUtils
import forms.OverseasCompanyRegistrationForm._
import models.{Identification, OverseasCompanyRegistration}
import play.api.Logger
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

object OverseasCompanyRegistrationController extends OverseasCompanyRegistrationController {
  val subscriptionDataService = SubscriptionDataService
  val delegationConnector = FrontendDelegationConnector
}


trait OverseasCompanyRegistrationController extends AtedBaseController with AtedFrontendAuthHelpers with DelegationAwareActions {

  def subscriptionDataService: SubscriptionDataService

  def edit = AuthAction(AtedRegime) {
    implicit atedContext =>
      for {
        overseasCompanyRegistration <- subscriptionDataService.getOverseasCompanyRegistration
      } yield {
        val result = OverseasCompanyRegistration(overseasCompanyRegistration.map(_.idNumber), overseasCompanyRegistration.map(_.issuingInstitution), overseasCompanyRegistration.map(_.issuingCountryCode))
        Ok(views.html.subcriptionData.overseasCompanyRegistration(overseasCompanyRegistrationForm.fill(result), CountryCodeUtils.getIsoCodeTupleList, getBackLink))
      }
  }

  def submit = AuthAction(AtedRegime) {
    implicit atedContext =>
      overseasCompanyRegistrationForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.subcriptionData.overseasCompanyRegistration(formWithErrors, CountryCodeUtils.getIsoCodeTupleList, getBackLink))),
        data => {
          for {
            _ <- subscriptionDataService.updateOverseasCompanyRegistration(Identification(data.businessUniqueId.getOrElse(""), data.issuingInstitution.getOrElse(""), data.countryCode.getOrElse("")))
          } yield {
            Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
          }
        }
      )
  }

  private def getBackLink() = {
    Some(routes.CompanyDetailsController.view().url)
  }
}
