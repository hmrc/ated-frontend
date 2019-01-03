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

package controllers.reliefs

import models.AtedContext
import play.api.mvc.Result
import play.api.mvc.Results._
import services.ReliefsService
import utils.{PeriodUtils, AtedUtils}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

trait ReliefHelpers {

  def reliefsService: ReliefsService

  def validatePeriodKey(periodKey: Int)(block: Future[Result])(implicit atedContext: AtedContext, hc: HeaderCarrier): Future[Result] = {
    if (PeriodUtils.calculatePeriod(month = 3) >= periodKey) {
      block
    } else {
      for {
        _ <- reliefsService.clearDraftReliefs
      } yield BadRequest(views.html.reliefs.invalidPeriodKey())
    }
  }

}
