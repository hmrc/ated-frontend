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

package controllers

import akka.actor.ActorSystem
import config.ApplicationConfig
import models.StandardAuthRetrievals
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.Injector
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, MessagesControllerComponents}
import play.api.test.FakeRequest
import testhelpers.TestUtil
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class ControllerBaseSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach with TestUtil {

  lazy val injector: Injector = app.injector
  lazy val messagesApi: MessagesApi = injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(Seq(Lang("en")))
  val mcc: MessagesControllerComponents = injector.instanceOf[MessagesControllerComponents]
  val migrationToETMP = "customerMigratedToETMPDate"
  val agentStandardRetrievals: StandardAuthRetrievals = StandardAuthRetrievals(agentEnrolmentSet, Some(agentAffinity), Some(delegationModel))
  val organisationStandardRetrievals: StandardAuthRetrievals = StandardAuthRetrievals(defaultEnrolmentSet, Some(organisationAffinity), Some(delegationModel))

  implicit val ec: ExecutionContext = injector.instanceOf[ExecutionContext]
  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit val system: ActorSystem = ActorSystem()
  implicit lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val hc = HeaderCarrier()


  lazy val fakeRequestWithSession: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
    "ts" -> "1498236506662",
    "authToken" -> "Bearer Token",
    migrationToETMP -> "2018-01-01"
  )

  def fakeRequestToPOSTWithSession(input: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] =
    fakeRequestWithSession.withFormUrlEncodedBody(input: _*)

}
