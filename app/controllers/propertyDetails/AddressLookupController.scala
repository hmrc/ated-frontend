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

package controllers.propertyDetails

import audit.Auditable
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.ControllerIds
import controllers.auth.{AuthAction, ClientHelper}
import forms.AddressLookupForms._
import javax.inject.Inject
import models.{AddressLookup, AddressSearchResults, PropertyDetailsAddress}
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.AtedUtils

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupController @Inject()(mcc: MessagesControllerComponents,
                                        auditConnector: DefaultAuditConnector,
                                        addressLookupService: AddressLookupService,
                                        authAction: AuthAction,
                                        confirmAddressController: ConfirmAddressController,
                                        val backLinkCacheConnector: BackLinkCacheConnector,
                                        val propertyDetailsService: PropertyDetailsService,
                                        val dataCacheConnector: DataCacheConnector)
                                       (implicit val appConfig: ApplicationConfig)
extends FrontendController(mcc) with PropertyDetailsHelpers with ClientHelper with Auditable with ControllerIds {

  implicit val ec: ExecutionContext = mcc.executionContext

  val controllerId: String = addressLookupId
  val appName: String = "ated-frontend"

  val audit: Audit = new Audit(s"ATED:$appName-AddressLookup", auditConnector)

  def view(id: Option[String], periodKey: Int, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        currentBackLink.map(backLink =>
          Ok(views.html.propertyDetails.addressLookup(id, periodKey, addressLookupForm, mode, backLink))
        )
      }
    }
  }

  def find(id: Option[String], periodKey: Int, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        addressLookupForm.bindFromRequest.fold(
          formWithError => {
            currentBackLink.flatMap(backLink =>
              Future.successful(BadRequest(views.html.propertyDetails.addressLookup(id, periodKey, formWithError, mode, backLink)))
            )
          },
          searchCriteria => {
            val trimmedPostCode = AtedUtils.formatMandatoryPostCode(searchCriteria.postcode)
            val trimmedSearchCriteria = searchCriteria.copy(postcode = trimmedPostCode)
            addressLookupService.find(trimmedSearchCriteria).flatMap { results =>
              val backToViewLink = Some(routes.AddressLookupController.view(id, periodKey, mode).url)
              Future.successful(Ok(views.html.propertyDetails.addressLookupResults(id, periodKey, addressSelectedForm, results, mode, backToViewLink)))
            }
          }
        )
      }
    }
  }

  def manualAddressRedirect(id: Option[String], periodKey: Int, mode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        val redirectUrl = id match {
          case Some(x) => controllers.propertyDetails.routes.PropertyDetailsAddressController.view(x, fromConfirmAddressPage = false, periodKey, mode)
          case None => controllers.propertyDetails.routes.PropertyDetailsAddressController.createNewDraft(periodKey)
        }
        redirectWithBackLinkDontOverwriteOldLink(
          propertyDetailsAddressId,
          redirectUrl,
          Some(controllers.propertyDetails.routes.AddressLookupController.view(id, periodKey, mode).url)
        )
      }
    }
  }


  def save(id: Option[String], periodKey: Int, mode: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authAction.authorisedAction { implicit authContext =>
      ensureClientContext {
        val backToViewLink = Some(routes.AddressLookupController.view(id, periodKey, mode).url)
        addressSelectedForm.bindFromRequest.fold(
          formWithError => {
            addressLookupService.retrieveCachedSearchResults.map { results =>
              val searchResults = results.fold(new AddressSearchResults(AddressLookup("", None), Nil))(a => a)
              BadRequest(views.html.propertyDetails.addressLookupResults(id, periodKey, formWithError, searchResults, mode, backToViewLink))
            }
          },
          searchCriteria => {
            addressLookupService.findById(searchCriteria.selected.get).flatMap(foundProperty =>
              (id, foundProperty) match {
                case (Some(x), Some(found)) =>
                  propertyDetailsService.saveDraftPropertyDetailsAddress(x, found).flatMap(
                    _ => {
                      auditInputAddress(found)
                      redirectWithBackLink(
                        confirmAddressId,
                        controllers.propertyDetails.routes.ConfirmAddressController.view(x, periodKey, mode),
                        backToViewLink
                      )
                    }
                  )
                case (None, Some(found)) =>
                  propertyDetailsService.createDraftPropertyDetailsAddress(periodKey, found).flatMap {
                    newId =>
                      auditInputAddress(found)
                      redirectWithBackLink(
                        confirmAddressId,
                        controllers.propertyDetails.routes.ConfirmAddressController.view(newId, periodKey,mode),
                        backToViewLink
                      )
                  }
                case _ =>
                  addressLookupService.retrieveCachedSearchResults.map { results =>
                    val searchResults = results.fold(new AddressSearchResults(AddressLookup("", None), Nil))(a => a)
                    val errorForm = addressSelectedForm.fill(searchCriteria)
                      .withError(FormError("selected", Messages("ated.address-lookup.error.general.selected-address")))
                    BadRequest(views.html.propertyDetails.addressLookupResults(id, periodKey, errorForm, searchResults, mode, backToViewLink))
                  }
              }
            )
          }
        )
      }
    }
  }

  def auditInputAddress(address: PropertyDetailsAddress)(implicit hc: HeaderCarrier): Unit = {
    sendDataEvent("postcodeAddressSubmitted", detail = Map(
      "submittedLine1" -> address.line_1,
      "submittedLine2" -> address.line_2,
      "submittedLine3" -> address.line_3.getOrElse(""),
      "submittedLine4" -> address.line_4.getOrElse(""),
      "submittedPostcode" -> address.postcode.getOrElse(""),
      "submittedCountry" -> "UK")) //country is Hardcoded as UK as all property should be.
  }
}


