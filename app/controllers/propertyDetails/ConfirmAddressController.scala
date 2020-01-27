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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AddressLookupService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService}
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class ConfirmAddressController @Inject()(mcc: MessagesControllerComponents,
                                         auditConnector: DefaultAuditConnector,
                                         addressLookupService: AddressLookupService,
                                         authAction: AuthAction,
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
        val backToViewLink = Some(routes.AddressLookupController.view(Some(id), periodKey, mode).url)
        propertyDetailsService.retrieveDraftPropertyDetails(id).map {
          case successResponse: PropertyDetailsCacheSuccessResponse =>
            val addressProperty = successResponse.propertyDetails.addressProperty
            Ok(views.html.propertyDetails.confirmAddress(id, periodKey, addressProperty, mode, backToViewLink))
          case _ =>
            Ok(views.html.global_error("", "", "", None, None, None, appConfig))
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



