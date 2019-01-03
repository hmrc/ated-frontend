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

package utils

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class FeatureSwitchSpec extends PlaySpec with OneServerPerSuite with BeforeAndAfterEach {

  override def beforeEach = {
    System.clearProperty("feature.test")
  }

  "FeatureSwitch" should {

    "generate correct system property name for the feature" in {
      FeatureSwitch.systemPropertyName("test") must be("features.test")
    }

    "be ENABLED if the system property is defined as 'true'" in {
      System.setProperty("features.test", "true")

      FeatureSwitch.forName("test").enabled must be(true)
    }

    "be DISABLED if the system property is defined as 'false'" in {
      System.setProperty("features.test", "false")

      FeatureSwitch.forName("test").enabled must be(false)
    }

    "be DISABLED if the system property is undefined" in {
      System.clearProperty("features.test")

      FeatureSwitch.forName("test").enabled must be(false)
    }

    "support dynamic toggling" in {
      System.setProperty("features.test", "false")
      val testFeatureSwitch = FeatureSwitch("test", enabled = true)
      FeatureSwitch.enable(testFeatureSwitch)
      FeatureSwitch.forName("test").enabled must be(true)

      FeatureSwitch.disable(testFeatureSwitch)
      FeatureSwitch.forName("test").enabled must be(false)
    }
  }

}
