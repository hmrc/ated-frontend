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
import forms.AtedForms
import forms.AtedForms._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import services.SubscriptionDataService
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import utils.{AtedUtils, CountryCodeUtils}

import scala.concurrent.Future

trait CorrespondenceAddressController extends AtedBaseController with AtedFrontendAuthHelpers with DelegationAwareActions {

  def subscriptionDataService: SubscriptionDataService

  def editAddress = AuthAction(AtedRegime) {
    implicit atedContext =>
      val correspondenceAddressResponse = subscriptionDataService.getCorrespondenceAddress
      for {
        correspondenceAddress <- correspondenceAddressResponse
      } yield {
        val populatedForm = correspondenceAddress match {
          case Some(x) => correspondenceAddressForm.fill(x.addressDetails)
          case None => correspondenceAddressForm
        }
        Ok(views.html.subcriptionData.correspondenceAddress(populatedForm, CountryCodeUtils.getIsoCodeTupleList, getBackLink))
      }
  }

  def submit = AuthAction(AtedRegime) {
    implicit atedContext =>
      AtedForms.verifyUKPostCode(correspondenceAddressForm.bindFromRequest).fold(
        formWithErrors => Future.successful(BadRequest(views.html.subcriptionData.correspondenceAddress(formWithErrors,
          CountryCodeUtils.getIsoCodeTupleList, getBackLink))),
        addressData => {
          val trimmedPostCode = AtedUtils.formatPostCode(addressData.postalCode)
          val trimmedAddress = addressData.copy(postalCode = trimmedPostCode)
          for {
            correspondenceAddress <- subscriptionDataService.updateCorrespondenceAddressDetails(trimmedAddress)
          }
            yield {
            correspondenceAddress match {
              case Some(x) => Redirect(controllers.subscriptionData.routes.CompanyDetailsController.view())
              case None =>
                val errorMsg = Messages("ated.correspondence-address.save.error")
                val errorForm = correspondenceAddressForm.withError(key = "addressType", message = errorMsg).fill(addressData)
                BadRequest(views.html.subcriptionData.correspondenceAddress(errorForm, CountryCodeUtils.getIsoCodeTupleList, getBackLink))
            }
          }
        }
      )
  }

  private def getBackLink = {
    Some(controllers.subscriptionData.routes.CompanyDetailsController.view.url)
  }
}

object CorrespondenceAddressController extends CorrespondenceAddressController {
  val delegationConnector = FrontendDelegationConnector
  val dataCacheConnector = DataCacheConnector
  val subscriptionDataService = SubscriptionDataService
}
