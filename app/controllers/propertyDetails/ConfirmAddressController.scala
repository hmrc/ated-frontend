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

import audit.Auditable
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.ControllerIds
import controllers.auth.{AuthAction, ClientHelper}
import javax.inject.Inject
import models.SelectPeriod
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AddressLookupService, ChangeLiabilityReturnService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants.{RetrieveSelectPeriodFormId, SelectedPreviousReturn}
import utils.AtedUtils

import scala.concurrent.ExecutionContext

class ConfirmAddressController @Inject()(mcc: MessagesControllerComponents,
                                         auditConnector: DefaultAuditConnector,
                                         addressLookupService: AddressLookupService,
                                         authAction: AuthAction,
                                         changeLiabilityReturnService: ChangeLiabilityReturnService,
                                         val backLinkCacheConnector: BackLinkCacheConnector,
                                         val propertyDetailsService: PropertyDetailsService,
                                         val dataCacheConnector: DataCacheConnector)
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
        val backLink = { mode match {
          case mode if AtedUtils.getPropertyDetailsPreHeader(mode).contains("change") =>
            Some(controllers.propertyDetails.routes.PropertyDetailsAddressController.view(id,false,periodKey,mode).url)
          case mode if AtedUtils.isEditSubmittedMode(mode) =>
           Some(controllers.propertyDetails.routes.SelectExistingReturnAddressController.view(periodKey, "charge").url)
        case _ =>
           Some(controllers.propertyDetails.routes.AddressLookupController.view(None, periodKey, mode).url)
          }
        }

        propertyDetailsService.retrieveDraftPropertyDetails(id).map {
            case successResponse: PropertyDetailsCacheSuccessResponse =>
              val addressProperty = successResponse.propertyDetails.addressProperty
              Ok(views.html.propertyDetails.confirmAddress(id, periodKey, addressProperty, mode, backLink))
            case _ =>
              Ok(views.html.global_error("ated.generic.error.title", "ated.generic.error.header", "ated.generic.error.message", None, None, None, appConfig))
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
          backLink <- currentBackLink
        } yield {
          changeLiabilityReturnOpt match {
            case Some(x) =>
              Ok(views.html.propertyDetails.confirmAddress(
                x.id,
                x.periodKey,
                x.addressProperty,
                AtedUtils.getEditSubmittedMode(x, answer),
                backLink))
            case None => Redirect(controllers.routes.AccountSummaryController.view())
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



