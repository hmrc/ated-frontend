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

import controllers.AtedBaseController
import models.StandardAuthRetrievals
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request, Result}
import services.DelegationService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait AuthAction extends AtedBaseController with AuthorisedFunctions {

  val origin: String = "ated-frontend"

  def loginParams(implicit request: Request[AnyContent]): Map[String, Seq[String]] = Map(
    "continue" -> Seq(ExternalUrls.loginURL),
    "origin" -> Seq(origin)
  )


  def unauthorisedUrl(isSa: Boolean = false): Result = Redirect(controllers.routes.ApplicationController.unauthorised(isSa).url)

  val delegationService: DelegationService

  def authorisedForNoEnrolments(body: StandardAuthRetrievals => Future[Result])
                                (implicit request: Request[AnyContent],
                  ex: ExecutionContext,
                  hc: HeaderCarrier,
                  messages: Messages): Future[Result] = {
    authorised(
      AffinityGroup.Organisation or AffinityGroup.Agent
    ).retrieve(
      Retrievals.allEnrolments and Retrievals.affinityGroup and Retrievals.internalId
    ){
      case enrolments ~ affinityGroup ~ Some(_) =>
          body(StandardAuthRetrievals(enrolments.enrolments, affinityGroup, None))
    } recover {
      case e: AuthorisationException => handleException(e)
    }
  }

  def validateAgainstSaEnrolment(enrolments: Enrolments): Boolean = {

    enrolments.enrolments.count{ enrolment =>
      enrolment.key match {
        case "HMRC-ATED-ORG" | "HMRC-AGENT-AGENT" | "IR-SA" => true
        case _                                              => false
      }
    } == 1 && enrolments.getEnrolment("IR-SA").isDefined
  }

  def authorisedAction(body: StandardAuthRetrievals => Future[Result])
                      (implicit request: Request[AnyContent],
                       ex: ExecutionContext,
                       hc: HeaderCarrier,
                       messages: Messages): Future[Result] = {
    authorised(
      (Enrolment("HMRC-ATED-ORG") or Enrolment("HMRC-AGENT-AGENT") or Enrolment("IR-SA")) and
      ConfidenceLevel.L50 and
      (AffinityGroup.Organisation or AffinityGroup.Agent)
    ).retrieve(
      Retrievals.allEnrolments and Retrievals.affinityGroup and Retrievals.internalId
    ){
      case enrolments ~ affinityGroup ~ Some(internalId) =>
        if (validateAgainstSaEnrolment(enrolments)) {
          Future.successful(unauthorisedUrl(true))
        } else {
          delegationService.delegationCall(internalId) flatMap { delegationModel =>
            body(StandardAuthRetrievals(enrolments.enrolments, affinityGroup, delegationModel))
          }
        }
    } recover {
      case e: AuthorisationException => handleException(e)
    }
  }


  def handleException(ae: AuthorisationException)(implicit request: Request[AnyContent]): Result  = ae match {
    case usa: UnsupportedAffinityGroup =>
      Logger.info("[AuthAction][handleException] DefaultAuthAction:Refine - Unsupported Affinity Group" + usa)
      unauthorisedUrl()
    case nas: NoActiveSession =>
      Logger.info("[AuthAction][handleException] DefaultAuthAction:Refine - NoActiveSession" + nas)
      Redirect(ExternalUrls.loginURL, loginParams)
    case e: AuthorisationException =>
      Logger.info("[AuthAction][handleException] DefaultAuthAction:Refine - AuthorisationException:" + e)
      unauthorisedUrl()
}

}


