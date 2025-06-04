/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.{BackLinkCacheService, DataCacheConnector}
import controllers.ControllerIds
import controllers.auth.{AuthAction, ClientHelper}
import forms.PropertyDetailsForms._
import javax.inject.Inject
import models.{PropertyDetailsAddress, SelectPeriod}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AtedConstants._
import utils.AtedUtils
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsAddressController @Inject()(mcc: MessagesControllerComponents,
                                                 auditConnector: DefaultAuditConnector,
                                                 authAction: AuthAction,
                                                 changeLiabilityReturnService: ChangeLiabilityReturnService,
                                                 serviceInfoService: ServiceInfoService,
                                                 val propertyDetailsService: PropertyDetailsService,
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val backLinkCacheConnector: BackLinkCacheService,
                                                 template: views.html.propertyDetails.propertyDetailsAddress)
                                                (implicit val appConfig: ApplicationConfig)

  extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with Auditable with ControllerIds with WithUnsafeDefaultFormBinding {

  implicit val ec: ExecutionContext = mcc.executionContext

  val appName: String = "ated-frontend"
  val controllerId: String = propertyDetailsAddressId
  val audit: Audit = new Audit(s"ATED:$appName-PropertyDetailsAddress", auditConnector)

  def editSubmittedReturn(oldFormBundleNo: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        for {
          answer <- dataCacheConnector.fetchAndGetData[Boolean](SelectedPreviousReturn)
          periodKey <- dataCacheConnector.fetchAndGetData[SelectPeriod](RetrieveSelectPeriodFormId)
          changeLiabilityReturnOpt <- changeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(oldFormBundleNo, answer, periodKey)
          serviceInfoContent <- serviceInfoService.getPartial
          backLink <- currentBackLink
        } yield {
          changeLiabilityReturnOpt match {
            case Some(x) =>
              Ok(template(
                Some(x.id),
                x.periodKey,
                propertyDetailsAddressForm.fill(x.addressProperty),
                AtedUtils.getEditSubmittedMode(x, answer), serviceInfoContent,
                backLink, oldFormBundleNo = Some(oldFormBundleNo), fromConfirmAddressPage = false))
            case None => Redirect(controllers.routes.AccountSummaryController.view)
          }
        }
      }
    }
  }

  def createNewDraft(periodKey: Int) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          dataCacheConnector.saveFormData[Boolean](SelectedPreviousReturn, false).flatMap { _ =>
            currentBackLink.map(backLink =>
              Ok(template(None, periodKey, propertyDetailsAddressForm, None, serviceInfoContent, backLink, fromConfirmAddressPage = false))
            )
          }
        }
      }
    }
  }

  def view(id: String, fromConfirmAddressPage: Boolean, periodKey: Int, mode: Option[String]) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          val backLinkView = {
            if (fromConfirmAddressPage) {
              Some(controllers.propertyDetails.routes.ConfirmAddressController.view(id, periodKey, mode).url)
            } else if (AtedUtils.getPropertyDetailsPreHeader(mode).contains("change")) {
              Some(controllers.editLiability.routes.EditLiabilityTypeController.editLiability(id, periodKey, true).url)
            } else {
              Some(controllers.propertyDetails.routes.AddressLookupController.view(Some(id), periodKey, mode).url)
            }
          }
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              Future.successful(Ok(template(
                Some(id),
                propertyDetails.periodKey,
                propertyDetailsAddressForm.fill(propertyDetails.addressProperty),
                AtedUtils.getEditSubmittedMode(propertyDetails, Some(AtedUtils.isPrevReturn(mode))), serviceInfoContent,
                backLinkView,
                fromConfirmAddressPage = fromConfirmAddressPage)
              ))
          }
        }
      }
    }
    }


  def editFromSummary(id: String): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          propertyDetailsCacheResponse(id) {
            case PropertyDetailsCacheSuccessResponse(propertyDetails) =>
              val mode = AtedUtils.getEditSubmittedMode(propertyDetails)
              Future.successful(Ok(template(
                Some(id),
                propertyDetails.periodKey,
                propertyDetailsAddressForm.fill(propertyDetails.addressProperty),
                mode,
                serviceInfoContent,
                AtedUtils.getSummaryBackLink(id, None),
                fromConfirmAddressPage = false)
              ))
          }
        }
      }
    }
  }

  def addressLookupRedirect(id: Option[String], periodKey: Int, mode: Option[String]) : Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        val returnUrl = id match {
          case Some(x) => Some(controllers.propertyDetails.routes.PropertyDetailsAddressController.view(x, fromConfirmAddressPage = false, periodKey, mode).url)
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

  def save(id: Option[String], periodKey: Int, mode: Option[String], fromConfirmAddressPage: Boolean): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        serviceInfoService.getPartial.flatMap { serviceInfoContent =>
          val backLink = {
            id match {
              case Some(id) if fromConfirmAddressPage =>
                Some(controllers.propertyDetails.routes.ConfirmAddressController.view(id, periodKey, mode).url)
              case _ =>
                Some(controllers.propertyDetails.routes.AddressLookupController.view(id, periodKey, mode).url)
            }
          }
          propertyDetailsAddressForm.bindFromRequest().fold(
            formWithError => {
              Future.successful(BadRequest(template(
                id,
                periodKey,
                formWithError,
                mode,
                serviceInfoContent,
                backLink,
                fromConfirmAddressPage = fromConfirmAddressPage)))
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
                        confirmAddressId,
                        controllers.propertyDetails.routes.ConfirmAddressController.view(x, periodKey, mode),
                        Some(controllers.propertyDetails.routes.PropertyDetailsAddressController.view(x, fromConfirmAddressPage, periodKey, mode).url)
                      )
                    }
                  )
                case None =>
                  propertyDetailsService.createDraftPropertyDetailsAddress(periodKey, trimmedAddressProperty).flatMap {
                    newId => {
                      auditInputAddress(trimmedAddressProperty, "create-address")
                      redirectWithBackLink(
                        confirmAddressId,
                        controllers.propertyDetails.routes.ConfirmAddressController.view(newId, periodKey, mode),
                        Some(controllers.propertyDetails.routes.PropertyDetailsAddressController.view(newId, fromConfirmAddressPage, periodKey, mode).url)
                      )
                    }
                  }
              }
            }
          )
        }
      }
    }
  }

  def auditInputAddress(address: PropertyDetailsAddress, addressEditMode: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
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


