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

package controllers.reliefs

import config.ApplicationConfig
import models.StandardAuthRetrievals
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Request, Result}
import services.ReliefsService
import utils.PeriodUtils
import scala.concurrent.Future

trait ReliefHelpers {

  def reliefsService: ReliefsService
  val templateInvalidPeriodKey: views.html.reliefs.invalidPeriodKey

  def
  validatePeriodKey(periodKey: Int)(block: Future[Result])(implicit authContext: StandardAuthRetrievals,
                                                                    request: Request[AnyContent],
                                                                    messages: Messages,
                                                                    appConfig: ApplicationConfig): Future[Result] =
    if (PeriodUtils.calculatePeakStartYear() >= periodKey) block else Future.successful(BadRequest(templateInvalidPeriodKey()))

}
