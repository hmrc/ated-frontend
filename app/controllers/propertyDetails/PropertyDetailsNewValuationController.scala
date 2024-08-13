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

import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController


import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsNewValuationController @Inject()(mcc: MessagesControllerComponents,
                                                      authAction: AuthAction,
                                                      serviceInfoService: ServiceInfoService,
                                                      template: views.html.propertyDetails.propertyDetailsNewValuation)
                                                     (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc)  with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  implicit lazy val messages: Messages = MessagesImpl(mcc.langs.availables.head, messagesApi)

  def view(): Action[AnyContent] = Action.async { implicit request =>
  authAction.authorisedAction{ implicit authContext =>
    Future.successful(Ok(template(propertyDetailsNewValuationForm)))
  }
  }

  def save(): Action[AnyContent] = Action.async { implicit request => {

    println(s"hello here ********************************* ${propertyDetailsNewValuationForm}")
    authAction.authorisedAction { implicit authContext =>
      serviceInfoService.getPartial.map {  serviceInfoContent =>
        propertyDetailsNewValuationForm.bindFromRequest().fold(
          formWithErrors => {
            println(s"hello here inside form with error ********************************* ${formWithErrors}")
            BadRequest(template(formWithErrors))
          },
          revaluedValue => Redirect(controllers.propertyDetails.routes.PropertyDetailsNewValuationController.view())
        )
      }
    }
  }
  }
}


