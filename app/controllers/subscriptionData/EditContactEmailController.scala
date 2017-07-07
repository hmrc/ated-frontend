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
import connectors.DataCacheConnector
import controllers.AtedBaseController
import controllers.auth.{AtedFrontendAuthHelpers, AtedRegime}
import forms.AtedForms._
import models.{AtedContext, EditContactDetails, EditContactDetailsEmail}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import services.SubscriptionDataService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions

import scala.concurrent.Future

trait EditContactEmailController extends AtedBaseController with AtedFrontendAuthHelpers with DelegationAwareActions {
  val subscriptionDataService: SubscriptionDataService

  def edit = AuthAction(AtedRegime) {
    implicit atedContext =>
      for {
        emailWithConsent <- subscriptionDataService.getEmailWithConsent
      } yield {
        val populatedForm = emailWithConsent match {
          case Some(x) => editContactDetailsEmailForm.fill(x)
          case _ => editContactDetailsEmailForm
        }
        Ok(views.html.subcriptionData.editContactEmail(populatedForm, getBackLink))
      }
  }


  def submit = AuthAction(AtedRegime) {
    implicit atedContext =>

      validateEmail(editContactDetailsEmailForm.bindFromRequest).fold(
        formWithErrors => Future.successful(BadRequest(views.html.subcriptionData.editContactEmail(formWithErrors, getBackLink))),
        editedClientData => {
          for {
            editedContact <- subscriptionDataService.editEmailWithConsent(editedClientData)
          } yield {
            Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
          }
        }
      )
  }

  private def getBackLink = {
    Some(controllers.subscriptionData.routes.CompanyDetailsController.view.url)
  }
}

object EditContactEmailController extends EditContactEmailController {
  val delegationConnector = FrontendDelegationConnector
  val dataCacheConnector = DataCacheConnector
  override val subscriptionDataService = SubscriptionDataService
}
