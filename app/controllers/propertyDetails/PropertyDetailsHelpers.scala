/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.BackLinkController
import controllers.auth.AtedFrontendAuthHelpers
import models.AtedContext
import play.api.mvc.Result
import services._
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions

import scala.concurrent.Future

trait PropertyDetailsHelpers extends BackLinkController with AtedFrontendAuthHelpers with DelegationAwareActions {

  def propertyDetailsService: PropertyDetailsService

  def propertyDetailsCacheResponse(id: String)(f: PartialFunction[PropertyDetailsCacheResponse, Future[Result]])
                                   (implicit atedContext: AtedContext): Future[Result] = {

    val handleError: PartialFunction[PropertyDetailsCacheResponse, Future[Result]] = {
      case PropertyDetailsCacheNotFoundResponse | PropertyDetailsCacheErrorResponse =>
        Future.successful(Redirect(controllers.routes.AccountSummaryController.view()))
    }

    propertyDetailsService.retrieveDraftPropertyDetails(id).flatMap(handleError orElse f)
  }

}
