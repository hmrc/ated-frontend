/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.auth.AuthAction
import forms.PropertyDetailsForms.propertyDetailsHasBeenRevaluedForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ServiceInfoService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.propertyDetails.propertyDetailsHasBeenRevalued

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsHasBeenRevaluedController @Inject()(mcc: MessagesControllerComponents,
                                                         authAction: AuthAction,
                                                         template: propertyDetailsHasBeenRevalued,
                                                         serviceInfoService: ServiceInfoService
                                                        )(
                                                        implicit val appConfig: ApplicationConfig
) extends FrontendController(mcc) {

  implicit val ec: ExecutionContext = mcc.executionContext
  def view(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      //ensureClientContext should go here
    if (appConfig.newRevaluedFeature) {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>

          //propertyDetailsCacheResponse here

          //using dummy periodKey '2024'
          Future.successful(Ok(template(id, 2024, None, None, propertyDetailsHasBeenRevaluedForm, serviceInfoContent)))
        }
      } else Future.successful(Redirect(controllers.routes.HomeController.home()))
    }
    }


  def save(id: String, periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      authAction.authorisedAction { implicit authContext =>

        //ensureClientContext should go here
        if (appConfig.newRevaluedFeature) {
          serviceInfoService.getPartial.map { serviceInfoContent =>
            propertyDetailsHasBeenRevaluedForm.bindFromRequest().fold(

              //using dummy periodKey '2024'
              formWithErrors => BadRequest(template(id, 2024, None, None, formWithErrors, serviceInfoContent)),

              //redirecting to original revalued control for now. Should ultimately redirect to 'date you made the £40000 change' controller
              hasBeenRevalued => Redirect(controllers.propertyDetails.routes.PropertyDetailsRevaluedController.view(id))
            )
          }
        } else Future.successful(Redirect(controllers.routes.HomeController.home()))
      }
  }
}
