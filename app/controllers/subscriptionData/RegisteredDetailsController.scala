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

import config.FrontendDelegationConnector
import connectors.DataCacheConnector
import controllers.AtedBaseController
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime}
import forms.AtedForms._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import services.SubscriptionDataService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import utils.CountryCodeUtils

import scala.concurrent.Future

trait RegisteredDetailsController extends AtedBaseController with AtedFrontendAuthHelpers with DelegationAwareActions {

  def subscriptionDataService: SubscriptionDataService

  def edit = AuthAction(AtedRegime) {
    implicit atedContext =>
      for {
        registeredDetails <- subscriptionDataService.getRegisteredDetails
      } yield {
        val populatedForm = registeredDetails match {
          case Some(x) => registeredDetailsForm.fill(x)
          case None => registeredDetailsForm
        }
        Ok(views.html.subcriptionData.registeredDetails(populatedForm, CountryCodeUtils.getIsoCodeTupleList, getBackLink))
      }
  }

  def submit() = AuthAction(AtedRegime) {
    implicit atedContext =>
      registeredDetailsForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.subcriptionData.registeredDetails(formWithErrors, CountryCodeUtils.getIsoCodeTupleList, getBackLink))),
        updateDetails => {
          for {
            registeredDetails <- subscriptionDataService.updateRegisteredDetails(updateDetails)
          } yield {
            registeredDetails match {
              case Some(x) => Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
              case None =>
                val errorMsg = Messages("ated.registered-details.save.error")
                val errorForm = registeredDetailsForm.withError(key = "addressType", message = errorMsg).fill(updateDetails)
                BadRequest(views.html.subcriptionData.registeredDetails(errorForm, CountryCodeUtils.getIsoCodeTupleList, getBackLink))
            }
          }
        }
      )
  }

  private def getBackLink = {
    Some(controllers.subscriptionData.routes.CompanyDetailsController.view.url)
  }
}

object RegisteredDetailsController extends RegisteredDetailsController {
  val delegationConnector = FrontendDelegationConnector
  val dataCacheConnector = DataCacheConnector
  val subscriptionDataService = SubscriptionDataService
}
