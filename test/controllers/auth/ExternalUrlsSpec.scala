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

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class ExternalUrlsSpec extends PlaySpec with OneServerPerSuite {

  "ExternalUrls" must {

    "have companyAuthHost " in {
      ExternalUrls.companyAuthHost must be("http://localhost:9025")
    }

    "have loginCallback " in {
      ExternalUrls.loginCallback must be("http://localhost:9916/ated/home")
    }

    "have loginPath " in {
      ExternalUrls.loginPath must be("sign-in")
    }

    "have loginURL " in {
      ExternalUrls.loginURL must be("http://localhost:9025/gg/sign-in")
    }

    "have continueURL " in {
      ExternalUrls.continueURL must be("http://localhost:9916/ated/home")
    }

    "have signIn " in {
      ExternalUrls.signIn must be( "http://localhost:9025/gg/sign-in?continue=http://localhost:9916/ated/home")
    }

    "have signOut " in {
      ExternalUrls.signOut must be("http://localhost:9025/gg/sign-out")
    }

    "have subscription redirect " in {
      ExternalUrls.subscriptionStartPage must be("http://localhost:9933/ated-subscription/start-subscription")
    }
  }

}
