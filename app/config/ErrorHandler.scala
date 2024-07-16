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

package config

import javax.inject.Inject
import play.api.Configuration
import play.api.i18n.{MessagesApi, Messages}
import play.api.mvc.RequestHeader
import play.twirl.api.Html
import scala.concurrent.Future
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

class ErrorHandler @Inject()(val messagesApi: MessagesApi,
                             val templateError: views.html.global_error,
                             val configuration: Configuration,
                             implicit val applicationConfig: ApplicationConfig, val ec: scala.concurrent.ExecutionContext) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)
                                    (implicit request: RequestHeader): Future[Html] = {
    Future.successful(templateError(pageTitle, heading, message, None, None, None, None, Html("")))
  }

  override def internalServerErrorTemplate(implicit request: RequestHeader): Future[Html] = {
    Future.successful(templateError(Messages("ated.generic.error.title"),
      Messages("ated.generic.error.header"),
      Messages("ated.generic.error.message"),
      Some(Messages("ated.generic.error.message2")),
      None,
      None,
      None,
      Html("")))
  }
}
