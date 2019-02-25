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

package controllers

trait ExistingReturnQuestionController extends AtedBaseController with AtedFrontendAuthHelpers with DelegationAwareActions with ClientHelper {

  def propertyDetailsService: PropertyDetailsService
  def reliefsService: ReliefsService

  def dataCacheConnector: DataCacheConnector

  def view(periodKey: Int, returnType: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        Future.successful(Ok(views.html.confirmPastReturn(new YesNoQuestionForm("client.agent-change.error").yesNoQuestionForm, periodKey,
          returnType, getBackLink(periodKey, returnType))))
      }
  }

  def submit(periodKey: Int, returnType: String) = AuthAction(AtedRegime) {
    implicit atedContext =>
      ensureClientContext {
        dataCacheConnector.saveFormData[SelectPeriod](RetrieveSelectPeriodFormId, SelectPeriod(Some(periodKey.toString)))
        val form = new YesNoQuestionForm("ated.confirm-past-return.error")
        form.yesNoQuestionForm.bindFromRequest.fold(
          formWithError =>
            Future.successful(BadRequest(views.html.confirmPastReturn(formWithError, periodKey, returnType, getBackLink(periodKey, returnType)))
            ),
          data => {
            val existingPastReturn = data.yesNo.getOrElse(false)
            if (existingPastReturn) {
              Future.successful(Redirect(controllers.propertyDetails.routes.SelectExistingReturnAddressController.view(periodKey, returnType)))
            } else {
              Future.successful(Redirect(controllers.propertyDetails.routes.AddressLookupController.view(None, periodKey)))
            }
          }
        )
      }
  }

  private def getBackLink(periodKey: Int, returnType: String) = Some(controllers.routes.ReturnTypeController.view(periodKey).url)
}

object ExistingReturnQuestionController extends ExistingReturnQuestionController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  val propertyDetailsService = PropertyDetailsService
  val reliefsService = ReliefsService
  val delegationConnector = FrontendDelegationConnector
  // $COVERAGE-ON$
}
