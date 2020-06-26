/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import forms.AddressLookupForms.addressSelectedForm
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{FormBundleReturnsService, PropertyDetailsService, ServiceInfoService, SummaryReturnsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants._

import scala.concurrent.{ExecutionContext, Future}

class SelectExistingReturnAddressController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      summaryReturnService: SummaryReturnsService,
                                                      confirmAddressController: ConfirmAddressController,
                                                      propertyDetailsAddressController: PropertyDetailsAddressController,
                                                      formBundleReturnService: FormBundleReturnsService,
                                                      serviceInfoService: ServiceInfoService,
                                                      val propertyDetailsService: PropertyDetailsService,
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val backLinkCacheConnector: BackLinkCacheConnector)
                                                     (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper {

  implicit val ec: ExecutionContext = mcc.executionContext
  val controllerId = "SelectExistingReturnAddressController"


  def view(periodKey: Int, returnType: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          previousReturns <- summaryReturnService.retrieveCachedPreviousReturnAddressList
          serviceInfoContent <- serviceInfoService.getPartial
        } yield {
          previousReturns match {
            case Some(pr) =>
              val uniqueAddresses = pr.groupBy(_.address).values.map(_.sortWith((a,b) => a.date.isAfter(b.date)).head).toSeq

              Ok(views.html.propertyDetails.selectPreviousReturn
            (periodKey, returnType, addressSelectedForm, uniqueAddresses, serviceInfoContent, getBackLink(periodKey, returnType)))
            case None => Ok(views.html.propertyDetails.selectPreviousReturn
            (periodKey, returnType, addressSelectedForm, Nil, serviceInfoContent, getBackLink(periodKey, returnType)))
          }
        }
      }
    }
  }

  def continueWithThisReturnRedirect(periodKey: Int, returnType: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        redirectWithBackLinkDontOverwriteOldLink(
          propertyDetailsAddressController.propertyDetailsAddressId,
          controllers.propertyDetails.routes.PropertyDetailsAddressController.createNewDraft(periodKey),
          Some(controllers.propertyDetails.routes.SelectExistingReturnAddressController.view(periodKey, returnType).url)
        )
      }
    }
  }

  def continue(periodKey: Int, returnType: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          addressSelectedForm.bindFromRequest.fold(
            formWithError => {
              summaryReturnService.retrieveCachedPreviousReturnAddressList.map { prevReturns =>
                val addressList = prevReturns.getOrElse(Nil)
                BadRequest(views.html.propertyDetails.selectPreviousReturn(periodKey, returnType, formWithError, addressList, serviceInfoContent, getBackLink(periodKey, returnType)))
              }
            },
            addressSelectForm => {
              val formBundleNum = addressSelectForm.selected.get
              for {
                formBundleReturnOpt <- formBundleReturnService.getFormBundleReturns(formBundleNum)
                result <- formBundleReturnOpt match {
                  case Some(_) =>
                    dataCacheConnector.saveFormData[Boolean](SelectedPreviousReturn, true).flatMap { _ =>
                      redirectWithBackLink(
                        confirmAddressController.controllerId,
                        controllers.propertyDetails.routes.ConfirmAddressController.editSubmittedReturn(formBundleNum),
                        Some(controllers.propertyDetails.routes.SelectExistingReturnAddressController.view(periodKey, returnType).url))
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
  }

  private def getBackLink(periodKey: Int, returnType: String) = {
    Some(controllers.routes.ExistingReturnQuestionController.view(periodKey = periodKey, returnType = returnType).url)
  }
}
