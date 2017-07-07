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

package controllers.propertyDetails

import config.{AtedFrontendAuditConnector, FrontendDelegationConnector}
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.{AtedRegime, ClientHelper}
import forms.AddressLookupForms._
import forms.PropertyDetailsForms
import forms.PropertyDetailsForms._
import models.{AddressLookup, AddressSearchResult, AddressSearchResults, PropertyDetailsAddress}
import play.api.data.FormError
import play.api.i18n.Messages
import services._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.AtedUtils
import audit.Auditable
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait AddressLookupController extends PropertyDetailsHelpers with ClientHelper with Auditable{

  val addressLookupService : AddressLookupService

  def view(id: Option[String], periodKey: Int, mode: Option[String] = None) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        currentBackLink.map(backLink =>
          Ok(views.html.propertyDetails.addressLookup(id, periodKey, addressLookupForm, mode, backLink))
        )
      }
  }

  def find(id: Option[String], periodKey: Int, mode: Option[String] = None) = AuthAction(AtedRegime) {
    implicit atedContext =>
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

  def manualAddressRedirect(id: Option[String], periodKey: Int, mode: Option[String]) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        val redirectUrl = id match {
          case Some(x) => controllers.propertyDetails.routes.PropertyDetailsAddressController.view(x)
          case None => controllers.propertyDetails.routes.PropertyDetailsAddressController.createNewDraft(periodKey)
        }
        RedirectWithBackLinkDontOverwriteOldLink(
          PropertyDetailsAddressController.controllerId,
          redirectUrl,
          Some(controllers.propertyDetails.routes.AddressLookupController.view(id, periodKey, mode).url)
        )
      }
  }


  def save(id: Option[String], periodKey: Int, mode: Option[String] = None) = AuthAction(AtedRegime) {
    implicit atedContext =>
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
            addressLookupService.findById(searchCriteria.selected).flatMap(foundProperty =>
              (id, foundProperty) match {
                case (Some(x), Some(found)) =>
                  propertyDetailsService.saveDraftPropertyDetailsAddress(x, found).flatMap(
                    _ => {
                      auditInputAddress(found)
                      RedirectWithBackLink(
                        PropertyDetailsTitleController.controllerId,
                        controllers.propertyDetails.routes.PropertyDetailsTitleController.view(x),
                        backToViewLink
                      )
                    }
                  )
                case (None, Some(found)) =>
                  propertyDetailsService.createDraftPropertyDetailsAddress(periodKey, found).flatMap {
                    newId =>
                      auditInputAddress(found)
                      RedirectWithBackLink(
                        PropertyDetailsTitleController.controllerId,
                        controllers.propertyDetails.routes.PropertyDetailsTitleController.view(newId),
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

object AddressLookupController extends AddressLookupController {
  val delegationConnector = FrontendDelegationConnector
  val propertyDetailsService = PropertyDetailsService
  val dataCacheConnector = DataCacheConnector
  val addressLookupService = AddressLookupService
  override val controllerId = "AddressLookupController"
  override val backLinkCacheConnector = BackLinkCacheConnector
  val audit: Audit = new Audit(s"ATED:${AppName.appName}-AddressLookup", AtedFrontendAuditConnector)
  val appName = AppName.appName
}
