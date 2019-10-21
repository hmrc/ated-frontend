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

package controllers.propertyDetails

import audit.Auditable
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AuthAction, ClientHelper}
import controllers.ControllerIds
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models.{PropertyDetailsAddress, SelectPeriod}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedConstants._
import utils.AtedUtils

import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsAddressController @Inject()(mcc: MessagesControllerComponents,
                                                 propertyDetailsTitleController: PropertyDetailsTitleController,
                                                 auditConnector: DefaultAuditConnector,
                                                 authAction: AuthAction,
                                                 changeLiabilityReturnService: ChangeLiabilityReturnService,
                                                 val propertyDetailsService: PropertyDetailsService,
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val backLinkCacheConnector: BackLinkCacheConnector)
                                                (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with Auditable with ControllerIds {

  implicit val ec: ExecutionContext = mcc.executionContext

  val appName: String = "ated-frontend"
  val controllerId: String = propertyDetailsAddressId
  val audit: Audit = new Audit(s"ATED:$appName-PropertyDetailsAddress", auditConnector)

  def editSubmittedReturn(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          answer <- dataCacheConnector.fetchAndGetFormData[Boolean](SelectedPreviousReturn)
          periodKey <- dataCacheConnector.fetchAndGetFormData[SelectPeriod](RetrieveSelectPeriodFormId)
          changeLiabilityReturnOpt <- changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo, answer, periodKey)
          backLink <- currentBackLink
        } yield {
          changeLiabilityReturnOpt match {
            case Some(x) =>
              Ok(views.html.propertyDetails.propertyDetailsAddress(
                Some(x.id),
                x.periodKey,
                propertyDetailsAddressForm.fill(x.addressProperty),
                AtedUtils.getEditSubmittedMode(x, answer),
                backLink, oldFormBundleNo = Some(oldFormBundleNo)))
            case None => Redirect(controllers.routes.AccountSummaryController.view())
          }
        }
      }
    }
  }

  def createNewDraft(periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        dataCacheConnector.saveFormData[Boolean](SelectedPreviousReturn, false).flatMap { _ =>
          currentBackLink.map(backLink =>
            Ok(views.html.propertyDetails.propertyDetailsAddress(None, periodKey, propertyDetailsAddressForm, None, backLink))
          )
        }
      }
    }
  }

  def view(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            currentBackLink.flatMap(backLink =>
              Future.successful(Ok(views.html.propertyDetails.propertyDetailsAddress(
                Some(id),
                propertyDetails.periodKey,
                propertyDetailsAddressForm.fill(propertyDetails.addressProperty),
                AtedUtils.getEditSubmittedMode(propertyDetails),
                backLink)
              )))
        }
      }
    }
  }

  def editFromSummary(id: String) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsCacheResponse(id) {
          case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
            val mode = AtedUtils.getEditSubmittedMode(propertyDetails)
            Future.successful(Ok(views.html.propertyDetails.propertyDetailsAddress(
              Some(id),
              propertyDetails.periodKey,
              propertyDetailsAddressForm.fill(propertyDetails.addressProperty),
              mode,
              AtedUtils.getSummaryBackLink(id, None))
            ))
          }
        }
      }
  }

  def addressLookupRedirect(id: Option[String], periodKey: Int, mode: Option[String]) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        val returnUrl = id match {
          case Some(x) => Some(controllers.propertyDetails.routes.PropertyDetailsAddressController.view(x).url)
          case None => Some(controllers.propertyDetails.routes.PropertyDetailsAddressController.createNewDraft(periodKey).url)
        }
        redirectWithBackLinkDontOverwriteOldLink(
          addressLookupId,
          controllers.propertyDetails.routes.AddressLookupController.view(id, periodKey, mode),
          returnUrl
        )
      }
    }
  }

  def save(id: Option[String], periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        propertyDetailsAddressForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.map(backLink =>
              BadRequest(views.html.propertyDetails.propertyDetailsAddress(id, periodKey, formWithError, mode, backLink))
            )
          },
          propertyDetails => {
            val trimmedPostCode = AtedUtils.formatPostCode(propertyDetails.postcode)
            val trimmedAddressProperty = propertyDetails.copy(postcode = trimmedPostCode)
            id match {
              case Some(x) =>
                propertyDetailsService.saveDraftPropertyDetailsAddress(x, trimmedAddressProperty).flatMap(
                  _ => {
                    auditInputAddress(trimmedAddressProperty, "edit-address")
                    redirectWithBackLink(
                      propertyDetailsTitleId,
                      controllers.propertyDetails.routes.PropertyDetailsTitleController.view(x),
                      Some(controllers.propertyDetails.routes.PropertyDetailsAddressController.view(x).url)
                    )
                  }
                )
              case None =>
                propertyDetailsService.createDraftPropertyDetailsAddress(periodKey, trimmedAddressProperty).flatMap {
                  newId => {
                    auditInputAddress(trimmedAddressProperty, "create-address")
                    redirectWithBackLink(
                      propertyDetailsTitleId,
                      controllers.propertyDetails.routes.PropertyDetailsTitleController.view(newId),
                      Some(controllers.propertyDetails.routes.PropertyDetailsAddressController.view(newId).url)
                    )
                  }
                }
            }
          }
        )
      }
    }
  }

  def auditInputAddress(address: PropertyDetailsAddress, addressEditMode: String)(implicit hc: HeaderCarrier): Unit = {
    val auditType = addressEditMode match {
      case "create-address" => "manualAddressSubmitted"
      case "edit-address" => "postcodeAddressModifiedSubmitted"
    }
    sendDataEvent(auditType, detail = Map(
      "submittedLine1" -> address.line_1,
      "submittedLine2" -> address.line_2,
      "submittedLine3" -> address.line_3.getOrElse(""),
      "submittedLine4" -> address.line_4.getOrElse(""),
      "submittedPostcode" -> address.postcode.getOrElse(""),
      "submittedCountry" -> "UK")) //country is Hardcoded as UK as all property should be.
  }

}


