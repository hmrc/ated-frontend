/*
 * Copyright 2021 HM Revenue & Customs
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

package config.featureswitch

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig


class FeatureSwitchingSpec extends PlaySpec with FeatureSwitching with MockitoSugar {
  override val conf: ServicesConfig = mock[ServicesConfig]

  FeatureSwitch.switches foreach { switch =>
    s"isEnabled(${switch.name})" should {
      "return true when a feature switch is set" in {
        enable(switch)
        isEnabled(switch) must be(true)
      }

      "return false when a feature switch is set to false" in {
        disable(switch)
        isEnabled(switch) must be(false)
      }

      "return false when a feature switch has not been set" in {
        sys.props -= switch.name
        sys.props.get(switch.name) must be(empty)
        isEnabled(switch) must  be(false)
      }
    }
  }
}
