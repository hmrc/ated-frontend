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

package controllers.propertyDetails

import config.FrontendDelegationConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedRegime, ClientHelper}
import forms.AddressLookupForms.addressSelectedForm
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.{FormBundleReturnsService, PropertyDetailsService, SummaryReturnsService}
import utils.AtedConstants._
import utils.AtedUtils

import scala.concurrent.Future

trait SelectExistingReturnAddressController extends PropertyDetailsHelpers with ClientHelper {

  def summaryReturnService: SummaryReturnsService

  def formBundleReturnService: FormBundleReturnsService

  def view(periodKey: Int, returnType: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        for {
          previousReturns <- summaryReturnService.retrieveCachedPreviousReturnAddressList
        } yield {
          previousReturns match {
            case Some(pr) => Ok(views.html.propertyDetails.selectPreviousReturn(periodKey, returnType, addressSelectedForm, pr, getBackLink(periodKey, returnType)))
            case None => Ok(views.html.propertyDetails.selectPreviousReturn(periodKey, returnType, addressSelectedForm, Nil, getBackLink(periodKey, returnType)))
          }

        }
      }
  }

  def continue(periodKey: Int, returnType: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        addressSelectedForm.bindFromRequest.fold(
          formWithError => {
            summaryReturnService.retrieveCachedPreviousReturnAddressList.map { prevReturns =>
              val addressList = prevReturns.getOrElse(Nil)
              BadRequest(views.html.propertyDetails.selectPreviousReturn(periodKey, returnType, addressSelectedForm, addressList, getBackLink(periodKey, returnType)))
            }
          },
          addressSelectForm => {
            val formBundleNum = addressSelectForm.selected
            for {
              formBundleReturnOpt <- formBundleReturnService.getFormBundleReturns(formBundleNum)
              result <- formBundleReturnOpt match {
                case Some(formBundleReturn) =>
                  dataCacheConnector.saveFormData[Boolean](SelectedPreviousReturn, true).flatMap { saved =>
                    RedirectWithBackLink(
                      PropertyDetailsAddressController.controllerId,
                      controllers.propertyDetails.routes.PropertyDetailsAddressController.editSubmittedReturn(formBundleNum),
                      getBackLink(periodKey, returnType))
                  }
                case None =>
                  Logger.warn(s"[SelectExistingReturnAddressController][continue] - form bundle return not found for form-bundle-no::$formBundleNum")
                  Future.successful(Redirect(controllers.routes.AccountSummaryController.view))
              }
            } yield result
          }
        )
      }
  }

  private def getBackLink(periodKey: Int, returnType: String) = {
    Some(controllers.routes.ExistingReturnQuestionController.view(periodKey = periodKey, returnType = returnType).url)
  }
}


object SelectExistingReturnAddressController extends SelectExistingReturnAddressController {
  val delegationConnector = FrontendDelegationConnector
  val formBundleReturnService = FormBundleReturnsService
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  val summaryReturnService: SummaryReturnsService = SummaryReturnsService
  override val controllerId = "SelectExistingReturnAddressController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}
