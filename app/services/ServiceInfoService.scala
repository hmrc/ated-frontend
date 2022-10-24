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

package services

import config.ApplicationConfig
import connectors.ServiceInfoPartialConnector
import models.StandardAuthRetrievals
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.mvc.Http.HeaderNames
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.PartialFactory
import views.html.{BtaNavigationLinks, service_info}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ServiceInfoService @Inject()(serviceInfoPartialConnector: ServiceInfoPartialConnector,
                                      service_info: service_info,
                                      btaNavigationLinks: BtaNavigationLinks)
                                      (implicit val messagesApi: MessagesApi, config: ApplicationConfig) extends HeaderCarrierConverter{

  def getPartial(implicit ec: ExecutionContext, authContext: StandardAuthRetrievals, request: Request[_]): Future[Html] = {
    val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val hc: HeaderCarrier = headerCarrier.copy(extraHeaders = headerCarrier.headers(Seq(HeaderNames.COOKIE)))
    val maybeNavLinks = serviceInfoPartialConnector.getNavLinks
    implicit val messages: Messages = messagesApi.preferred(request)

    if(authContext.isAgent){
      Future.successful(HtmlFormat.empty)
    } else {

        for {
          navLinks <- maybeNavLinks
        } yield {
          navLinks.map(n =>
            service_info(PartialFactory.partialList(n))).getOrElse(btaNavigationLinks())
        }
    }
  }
}
