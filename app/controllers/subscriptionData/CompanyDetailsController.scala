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

package controllers.subscriptionData

import config.ApplicationConfig
import controllers.auth.AuthAction
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{DetailsService, ServiceInfoService, SubscriptionDataService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class CompanyDetailsController @Inject()(mcc: MessagesControllerComponents,
                                         authAction: AuthAction,
                                         subscriptionDataService: SubscriptionDataService,
                                         serviceInfoService: ServiceInfoService,
                                         detailsDataService: DetailsService,
                                         template: views.html.subcriptionData.companyDetails)
                                        (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) {

  implicit val ec: ExecutionContext = mcc.executionContext

  def view : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      for {
        serviceInfoContent <- serviceInfoService.getPartial
        emailConsent <- subscriptionDataService.getEmailConsent
        correspondenceAddress <- subscriptionDataService.getCorrespondenceAddress
        registeredDetails <- subscriptionDataService.getRegisteredDetails
        safeId <- subscriptionDataService.getSafeId
        overseasCompanyRegistration <- subscriptionDataService.getOverseasCompanyRegistration
        clientMandateDetails <- detailsDataService.getClientMandateDetails(safeId.getOrElse(throw new RuntimeException("Could not get safeId")), "ated")
      } yield {
        Ok(template(correspondenceAddress,
          registeredDetails,
          emailConsent,
          clientMandateDetails,
          overseasCompanyRegistration,
          serviceInfoContent,
          Some(controllers.routes.AccountSummaryController.view().url)
        ))
      }
    }
  }

  def back: Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
    }
  }
}
