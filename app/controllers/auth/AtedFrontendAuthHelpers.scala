/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.auth

import connectors.DataCacheConnector
import controllers.AtedBaseController
import models._
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.frontend.auth.TaxRegime

import scala.concurrent.Future

trait AtedFrontendAuthHelpers extends AtedBaseController {

  //scalastyle:off method.name
  def AuthAction(taxRegime: TaxRegime) = new AuthAction(taxRegime)

  class AuthAction(taxRegime: TaxRegime) {

    def apply(f: AtedContext => Future[Result]): Action[AnyContent] = {
      AuthorisedFor(taxRegime = taxRegime, pageVisibility = GGConfidence).async {
        implicit authContext => implicit request =>
          f(AtedContext(request, AtedUser(authContext)))
      }
    }
  }


}
