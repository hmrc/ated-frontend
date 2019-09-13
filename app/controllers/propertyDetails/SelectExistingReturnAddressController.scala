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

package controllers.propertyDetails

import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.AddressLookupForms.addressSelectedForm
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, FormBundleReturnsService, PropertyDetailsService, SummaryReturnsService}
import utils.AtedConstants._

import scala.concurrent.Future

trait SelectExistingReturnAddressController extends PropertyDetailsHelpers with ClientHelper with AuthAction{

  def summaryReturnService: SummaryReturnsService

  def formBundleReturnService: FormBundleReturnsService

  def view(periodKey: Int, returnType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
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
  }

  def continue(periodKey: Int, returnType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      ensureClientContext {
        addressSelectedForm.bindFromRequest.fold(
          formWithError => {
            summaryReturnService.retrieveCachedPreviousReturnAddressList.map { prevReturns =>
              val addressList = prevReturns.getOrElse(Nil)
              BadRequest(views.html.propertyDetails.selectPreviousReturn(periodKey, returnType, formWithError, addressList, getBackLink(periodKey, returnType)))
            }
          },
          addressSelectForm => {
            val formBundleNum = addressSelectForm.selected.get
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
                  Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
              }
            } yield result
          }
        )
      }
    }
  }

  private def getBackLink(periodKey: Int, returnType: String) = {
    Some(controllers.routes.ExistingReturnQuestionController.view(periodKey = periodKey, returnType = returnType).url)
  }
}


object SelectExistingReturnAddressController extends SelectExistingReturnAddressController {
  val delegationService: DelegationService = DelegationService
  val formBundleReturnService: FormBundleReturnsService = FormBundleReturnsService
  val propertyDetailsService: PropertyDetailsService = PropertyDetailsService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val summaryReturnService: SummaryReturnsService = SummaryReturnsService
  override val controllerId = "SelectExistingReturnAddressController"
  override val backLinkCacheConnector: BackLinkCacheConnector = BackLinkCacheConnector
}
