/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class FeatureSwitchSpec extends PlaySpec with BeforeAndAfterEach with MockitoSugar {

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  class Setup {
    val testFeatureSwitchImpl: FeatureSwitchImpl = new FeatureSwitchImpl(mockAppConfig)
  }

  override def beforeEach: Unit = System.clearProperty("feature.test")

  "FeatureSwitch" should {
    "generate correct system property name for the feature" in new Setup {
      testFeatureSwitchImpl.systemPropertyName("test") must be("features.test")
    }

    "be ENABLED if the system property is defined as 'true'" in new Setup {
      System.setProperty("features.test", "true")

      testFeatureSwitchImpl.forName("test").enabled must be(true)
    }

    "be DISABLED if the system property is defined as 'false'" in new Setup {
      System.setProperty("features.test", "false")

      testFeatureSwitchImpl.forName("test").enabled must be(false)
    }

    "be DISABLED if the system property is undefined" in new Setup {
      System.clearProperty("features.test")

      testFeatureSwitchImpl.forName("test").enabled must be(false)
    }

    "support dynamic toggling" in new Setup {
      System.setProperty("features.test", "false")
      val testFeatureSwitch: FeatureSwitch = FeatureSwitch("test", enabled = true)
      testFeatureSwitchImpl.enable(testFeatureSwitch)
      testFeatureSwitchImpl.forName("test").enabled must be(true)

      testFeatureSwitchImpl.disable(testFeatureSwitch)
      testFeatureSwitchImpl.forName("test").enabled must be(false)
    }
  }
}
