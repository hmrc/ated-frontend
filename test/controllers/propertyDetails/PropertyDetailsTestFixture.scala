/*
 * Copyright 2024 HM Revenue & Customs
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

import builders.PropertyDetailsBuilder
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.{DateOfChange, HasBeenRevalued, PropertyDetailsNewValuation, PropertyDetailsRevalued}
import org.mockito.ArgumentMatchers.{any, eq => eqs}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.OK
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.MessagesControllerComponents
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants.{DelegatedClientAtedRefNumber, FortyThousandValueDateOfChange, HasPropertyBeenRevalued, propertyDetailsNewValuationValue}
import views.html.BtaNavigationLinks

import java.time.LocalDate
import scala.concurrent.Future

abstract class PropertyDetailsTestFixture extends PlaySpec with GuiceOneServerPerSuite with MockAuthUtil {


  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockNewValuationController: PropertyDetailsNewValuationController = mock[PropertyDetailsNewValuationController]
  val mockIsFullTaxPeriodController: IsFullTaxPeriodController = mock[IsFullTaxPeriodController]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]

  val mockDateOfChangeController: PropertyDetailsDateOfChangeController = mock[PropertyDetailsDateOfChangeController]
  val mockExitController: PropertyDetailsExitController = mock[PropertyDetailsExitController]

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  case class Setup(enrolmentSet: Set[Enrolment] = defaultEnrolmentSet) {
    setupAuthForOrganisation(enrolmentSet)
    setupCommonMockExpectations()
  }


  def setupDataCacheConnectorExpectations(newValuation: Some[BigDecimal], hasPropertyBeenRevalued: Some[Boolean], dateOfRevaluationChange: Some[LocalDate]) = {
    when(mockDataCacheConnector.fetchAndGetFormData[HasBeenRevalued](eqs(HasPropertyBeenRevalued))(any(), any())).thenReturn(Future.successful(Some(HasBeenRevalued(hasPropertyBeenRevalued))))
    when(mockDataCacheConnector.fetchAndGetFormData[PropertyDetailsNewValuation](eqs(propertyDetailsNewValuationValue))
      (any(), any()))
      .thenReturn(Future.successful(Some(PropertyDetailsNewValuation(newValuation))))
    when(mockDataCacheConnector.fetchAndGetFormData[DateOfChange](eqs(FortyThousandValueDateOfChange))
      (any(), any()))
      .thenReturn(Future.successful(Some(DateOfChange(dateOfRevaluationChange))))
  }

  def setupCommonMockExpectations() = {
    val customBtaNavigationLinks = btaNavigationLinksView()(messages, mockAppConfig)
    when(mockServiceInfoService.getPartial(any(), any(), any())).thenReturn(Future.successful(customBtaNavigationLinks))
    when(mockDataCacheConnector.fetchAtedRefData[String](eqs(DelegatedClientAtedRefNumber))(any(), any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](any())
      (any(), any())).thenReturn(Future.successful(None))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(any())(any())).thenReturn(Future.successful(None))
    when(mockBackLinkCacheConnector.saveBackLink(any(), any())(any())).thenReturn(Future.successful(None))
  }

  def setupPropertyDetailServiceMockExpectations() = {
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("z11 1zz")).copy(value = None)
    when(mockPropertyDetailsService.retrieveDraftPropertyDetails(any())(any(), any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    when(mockPropertyDetailsService.saveDraftPropertyDetailsRevalued(any(), any())(any(), any())).thenReturn(Future.successful(OK))

  }

  def verifySaveBackLinkIsCalled = {
    verify(mockBackLinkCacheConnector).saveBackLink(any(), any())(any())
  }

  def verifyDataCacheConnectorRetursHasBeenRevalued(revalued: String) = {
    verify(mockDataCacheConnector).fetchAndGetFormData[HasBeenRevalued](
      eqs(revalued)
    )(any(), any())
  }

  def verifyPropertyDetailsService(isPropertyRevalued: Option[Boolean], revaluedValue: Option[BigDecimal], revaluedDate: Option[LocalDate], partAcqDispDate: Option[LocalDate]) = {
    val expectedPropertyDetails = PropertyDetailsRevalued(
      isPropertyRevalued = isPropertyRevalued,
      revaluedValue = revaluedValue,
      revaluedDate = revaluedDate,
      partAcqDispDate = partAcqDispDate
    )
    verify(mockPropertyDetailsService).saveDraftPropertyDetailsRevalued(any(), eqs(expectedPropertyDetails))(any(), any())
  }

}
