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

import connectors.DataCacheConnector
import models.PropertyDetailsNewBuildDates
import play.api.mvc.MessagesControllerComponents
import services._
import scala.concurrent.{ExecutionContext, Future}
import models.{DateCouncilRegistered, DateFirstOccupied}
import utils.AtedConstants._
import play.api.Logging

trait StoreNewBuildDates extends Logging {
  val mcc: MessagesControllerComponents
  val propertyDetailsService: PropertyDetailsService
  val dataCacheConnector: DataCacheConnector
  implicit val ec: ExecutionContext

  def storeNewBuildDatesFromCache(id: String)
                                 (implicit hc: uk.gov.hmrc.http.HeaderCarrier, authContext: models.StandardAuthRetrievals): Future[Int] = {

    dataCacheConnector.fetchAndGetFormData[DateFirstOccupied](NewBuildFirstOccupiedDate).flatMap{ firstOccupied =>
      dataCacheConnector.fetchAndGetFormData[DateCouncilRegistered](NewBuildCouncilRegisteredDate).flatMap{ councilRegistered =>
        logger.info(s"Storing new build dates, firstOccupied: $firstOccupied, councilRegistered: $councilRegistered")
        propertyDetailsService.saveDraftPropertyDetailsNewBuildDates(
          id,
          PropertyDetailsNewBuildDates(firstOccupied.flatMap(_.dateFirstOccupied), councilRegistered.flatMap(_.dateCouncilRegistered)))
      }
    }
  }
}