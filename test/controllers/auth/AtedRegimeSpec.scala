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

package controllers.auth

import builders.AuthBuilder
import controllers.auth.AtedRegime._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts

class AtedRegimeSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "AtedRegime" must {

    "define isAuthorised" must {

      val accounts = mock[Accounts](RETURNS_DEEP_STUBS)

      "return true when the user is registered as an ated" in {
        when(accounts.agent).thenReturn(None)
        when(accounts.ated.isDefined).thenReturn(true)
        isAuthorised(accounts) must be(true)
      }

      "return true when the user is registered as an agent" in {
        val agentAccount = AuthBuilder.createAgentAuthContext("user1", "testName", Some("JARN1234567"))
        when(accounts.agent).thenReturn(agentAccount.principal.accounts.agent)
        isAuthorised(accounts) must be(true)
      }

      "return false when the user is not registered for org or as an agent" in {
        when(accounts.agent).thenReturn(None)
        when(accounts.ated.isDefined).thenReturn(false)
        isAuthorised(accounts) must be(false)
      }

    }

    "define the authentication type as the Ated GG" in {
      authenticationType must be(AtedGovernmentGateway)
    }

    "define the unauthorised landing page as /unauthorised" in {
      unauthorisedLandingPage.get must be("/ated/unauthorised")
    }

  }

}
