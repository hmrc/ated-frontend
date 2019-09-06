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

import connectors.DataCacheConnector
import controllers.AtedBaseController
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import services.{DelegationService, DetailsService, SubscriptionDataService}

import scala.concurrent.Future

trait CompanyDetailsController extends AtedBaseController with AuthAction {

  def subscriptionDataService: SubscriptionDataService
  def detailsDataService: DetailsService

  def view : Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      for {
        emailConsent <- subscriptionDataService.getEmailConsent
        correspondenceAddress <- subscriptionDataService.getCorrespondenceAddress
        registeredDetails <- subscriptionDataService.getRegisteredDetails
        safeId <- subscriptionDataService.getSafeId
        overseasCompanyRegistration <- subscriptionDataService.getOverseasCompanyRegistration
        clientMandateDetails <- detailsDataService.getClientMandateDetails(safeId.getOrElse(throw new RuntimeException("Could not get safeId")), "ated")
      } yield {
        Ok(views.html.subcriptionData.companyDetails(correspondenceAddress,
          registeredDetails,
          emailConsent,
          clientMandateDetails,
          overseasCompanyRegistration,
          Some(controllers.routes.AccountSummaryController.view().url)
        ))
      }
    }
  }

  def back: Action[AnyContent] = Action.async { implicit request =>
    authorisedAction { implicit authContext =>
      Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
    }
  }
}

object CompanyDetailsController extends CompanyDetailsController {
  val delegationService: DelegationService = DelegationService
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val subscriptionDataService: SubscriptionDataService = SubscriptionDataService
  val detailsDataService: DetailsService = DetailsService

}
