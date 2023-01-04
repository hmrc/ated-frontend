/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.ControllerIds
import controllers.auth.{AuthAction, ClientHelper}
import javax.inject.Inject
import models.SelectPeriod
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ChangeLiabilityReturnService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants.{RetrieveSelectPeriodFormId, SelectedPreviousReturn}
import utils.AtedUtils

import scala.concurrent.ExecutionContext

class ConfirmAddressController @Inject()(mcc: MessagesControllerComponents,
                                         authAction: AuthAction,
                                         changeLiabilityReturnService: ChangeLiabilityReturnService,
                                         serviceInfoService: ServiceInfoService,
                                         val backLinkCacheConnector: BackLinkCacheConnector,
                                         val propertyDetailsService: PropertyDetailsService,
                                         val dataCacheConnector: DataCacheConnector,
                                         template: views.html.propertyDetails.confirmAddress,
                                         templateError: views.html.global_error)
                                        (implicit val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with ControllerIds {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = confirmAddressId
  val appName: String = "ated-frontend"

  def view(id: String,
           periodKey: Int,
           mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          val backLink = {
            mode match {
              case mode if AtedUtils.getPropertyDetailsPreHeader(mode).contains("change") =>
                Some(controllers.propertyDetails.routes.PropertyDetailsAddressController.view(id, false, periodKey, mode).url)
              case mode if AtedUtils.isEditSubmittedMode(mode) =>
                Some(controllers.propertyDetails.routes.SelectExistingReturnAddressController.view(periodKey, "charge").url)
              case _ =>
                Some(controllers.propertyDetails.routes.AddressLookupController.view(None, periodKey, mode).url)
            }
          }

          propertyDetailsService.retrieveDraftPropertyDetails(id).map {
            case successResponse: PropertyDetailsCacheSuccessResponse =>
              val addressProperty = successResponse.propertyDetails.addressProperty
              Ok(template(id, periodKey, addressProperty, mode, serviceInfoContent, backLink))
            case _ =>
              Ok(templateError("ated.generic.error.title", "ated.generic.error.header",
                "ated.generic.error.message", Some("ated.generic.error.message2"), None, None, None, serviceInfoContent))
          }
        }
      }
    }
    }

  def editSubmittedReturn(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          answer <- dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn)
          periodKey <- dataCacheConnector.fetchAndGetFormData[SelectPeriod](RetrieveSelectPeriodFormId)
          changeLiabilityReturnOpt <- changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo, answer, periodKey)
          serviceInfoContent <- serviceInfoService.getPartial
          backLink <- currentBackLink
        } yield {
          changeLiabilityReturnOpt match {
            case Some(x) =>
              Ok(template(
                x.id,
                x.periodKey,
                x.addressProperty,
                AtedUtils.getEditSubmittedMode(x, answer),
                serviceInfoContent,
                backLink))
            case None => Redirect(controllers.routes.AccountSummaryController.view)
          }
        }
      }
    }
  }

  def submit(id: String, periodKey: Int, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        val backToViewLink = Some(routes.ConfirmAddressController.view(id, periodKey, mode).url)
        redirectWithBackLink(
          propertyDetailsTitleId,
          controllers.propertyDetails.routes.PropertyDetailsTitleController.view(id),
          backToViewLink
        )
      }
    }
  }

}



