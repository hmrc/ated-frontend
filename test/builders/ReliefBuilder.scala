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

package builders

import models.{Reliefs, ReliefsTaxAvoidance, TaxAvoidance}
import utils.{PeriodUtils, AtedUtils}


object ReliefBuilder {

  def reliefTaxAvoidance(periodKey: Int): ReliefsTaxAvoidance = {
    reliefTaxAvoidance(periodKey, Reliefs(periodKey), TaxAvoidance())
  }


  def reliefTaxAvoidance(periodKey: Int,
                         reliefs: Reliefs,
                         taxAvoidance: TaxAvoidance = TaxAvoidance(equityReleaseScheme = Some("11111111"))): ReliefsTaxAvoidance = {
    ReliefsTaxAvoidance(periodKey,
      periodStartDate = PeriodUtils.periodStartDate(periodKey),
      periodEndDate = PeriodUtils.periodEndDate(periodKey),
      reliefs = reliefs,
      taxAvoidance = taxAvoidance
    )
  }

}
