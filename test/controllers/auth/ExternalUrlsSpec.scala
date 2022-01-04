/*
 * Copyright 2022 HM Revenue & Customs
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

import config.ApplicationConfig
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class ExternalUrlsSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar {

  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  "ExternalUrls" must {

    "have companyAuthHost " in {
      mockAppConfig.basGatewayHost must be("http://localhost:9553")
    }

    "have loginCallback " in {
      mockAppConfig.loginCallback must be("http://localhost:9916/ated/home")
    }

    "have loginPath " in {
      mockAppConfig.loginPath must be("sign-in")
    }

    "have loginURL " in {
      mockAppConfig.loginURL must be("http://localhost:9553/bas-gateway/sign-in")
    }

    "have continueURL " in {
      mockAppConfig.continueURL must be("http://localhost:9916/ated/home")
    }

    "have signIn " in {
      mockAppConfig.signIn must be("http://localhost:9553/bas-gateway/sign-in?continue_url=http://localhost:9916/ated/home")
    }

    "have signOut " in {
      mockAppConfig.signOut must be("http://localhost:9553/bas-gateway/sign-out-without-state")
    }

    "have subscription redirect " in {
      mockAppConfig.subscriptionStartPage must be("http://localhost:9933/ated-subscription/start-subscription")
    }
  }

}
