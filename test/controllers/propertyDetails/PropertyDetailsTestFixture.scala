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
import controllers.auth.AuthAction
import models.{DateOfChange, DateOfRevalue, HasBeenRevalued, PropertyDetailsNewValuation, PropertyDetailsRevalued}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => eqs}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.OK
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.MessagesControllerComponents
import services.{BackLinkCacheService, DataCacheService, PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants.{DateOfRevalueConstant, DelegatedClientAtedRefNumber, FortyThousandValueDateOfChange, HasPropertyBeenRevalued, SelectedPreviousReturn, propertyDetailsNewValuationValue}
import views.html.BtaNavigationLinks

import java.time.LocalDate
import scala.concurrent.Future

abstract class PropertyDetailsTestFixture extends PlaySpec with GuiceOneServerPerSuite with MockAuthUtil {

  given mockAppConfig: ApplicationConfig                         = mock[ApplicationConfig]
  stubServiceNavigationUrls(mockAppConfig)
  given hc: HeaderCarrier                                   = HeaderCarrier()
  val mockMcc: MessagesControllerComponents                             = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService                = mock[PropertyDetailsService]
  val mockDataCacheService: DataCacheService                        = mock[DataCacheService]
  val mockBackLinkCacheService: BackLinkCacheService                  = mock[BackLinkCacheService]
  val mockNewValuationController: PropertyDetailsNewValuationController = mock[PropertyDetailsNewValuationController]
  val mockIsFullTaxPeriodController: IsFullTaxPeriodController          = mock[IsFullTaxPeriodController]
  given messages: MessagesImpl                              = MessagesImpl(Lang("en-GB"), messagesApi)
  val messagesApi: MessagesApi                                          = app.injector.instanceOf[MessagesApi]
  val btaNavigationLinksView: BtaNavigationLinks                        = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService                        = mock[ServiceInfoService]

  val mockDateOfChangeController: PropertyDetailsDateOfChangeController = mock[PropertyDetailsDateOfChangeController]
  val mockExitController: PropertyDetailsExitController                 = mock[PropertyDetailsExitController]

  val mockAuthAction: AuthAction = new AuthAction(
    mockAppConfig,
    mockDelegationService,
    mockAuthConnector
  )

  case class Setup(enrolmentSet: Set[Enrolment] = defaultEnrolmentSet) {
    setupAuthForOrganisation(enrolmentSet)
    setupCommonMockExpectations()
  }

  def setupDataCacheServiceExpectations(newValuation: Some[BigDecimal],
                                          hasPropertyBeenRevalued: Some[Boolean],
                                          dateOfRevaluationChange: Some[LocalDate]) = {
    when(mockDataCacheService.fetchAndGetData[PropertyDetailsNewValuation](eqs(propertyDetailsNewValuationValue))(using any(), any()))
      .thenReturn(Future.successful(Some(PropertyDetailsNewValuation(newValuation))))
    when(mockDataCacheService.fetchAndGetData[DateOfChange](eqs(FortyThousandValueDateOfChange))(using any(), any()))
      .thenReturn(Future.successful(Some(DateOfChange(dateOfRevaluationChange))))

    when(mockDataCacheService.fetchAndGetData[HasBeenRevalued](eqs(HasPropertyBeenRevalued))(using any(), any()))
      .thenReturn(Future.successful(Some(HasBeenRevalued(hasPropertyBeenRevalued))))

    when(mockDataCacheService.fetchAndGetData[HasBeenRevalued](eqs(HasPropertyBeenRevalued))(using any(), any()))
      .thenReturn(Future.successful(Some(HasBeenRevalued(hasPropertyBeenRevalued))))
  }

  def setupCommonMockExpectations() = {
    val customBtaNavigationLinks = btaNavigationLinksView()(messages, mockAppConfig)
    when(mockServiceInfoService.getPartial(using any(), any(), any())).thenReturn(Future.successful(customBtaNavigationLinks))

    when(mockDataCacheService.fetchAndGetData[String](eqs(DelegatedClientAtedRefNumber))(using any(), any()))
      .thenReturn(Future.successful(Some("XN1200000100001")))

    when(mockDataCacheService.fetchAndGetData[Boolean](ArgumentMatchers.eq(SelectedPreviousReturn))(using ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(true)))

    when(mockDataCacheService.fetchAndGetData[PropertyDetailsNewValuation](eqs(propertyDetailsNewValuationValue))(using ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(PropertyDetailsNewValuation(Some(BigDecimal(1))))))

    when(mockDataCacheService.fetchAndGetData[DateOfRevalue](eqs(DateOfRevalueConstant))(using any(), any()))
      .thenReturn(Future.successful(Some(DateOfRevalue(Some(LocalDate.now())))))

    when(mockDataCacheService.fetchAndGetData[HasBeenRevalued](eqs(HasPropertyBeenRevalued))(using any(), any()))
      .thenReturn(Future.successful(Some(HasBeenRevalued(Some(true)))))

    when(mockDataCacheService.fetchAndGetData[DateOfChange](eqs(FortyThousandValueDateOfChange))(using any(), any()))
      .thenReturn(Future.successful(Some(DateOfChange(Some(LocalDate.now())))))

    when(mockBackLinkCacheService.fetchAndGetBackLink(any())(using any())).thenReturn(Future.successful(None))
    when(mockBackLinkCacheService.saveBackLink(any(), any())(using any())).thenReturn(Future.successful(None))
  }

  def setupPropertyDetailServiceMockExpectations() = {
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", "z11 1zz").copy(value = None)
    when(mockPropertyDetailsService.retrieveDraftPropertyDetails(any())(using any(), any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
    when(mockPropertyDetailsService.saveDraftPropertyDetailsRevalued(any(), any())(using any(), any())).thenReturn(Future.successful(OK))

  }

  def verifySaveBackLinkIsCalled = {
    verify(mockBackLinkCacheService).saveBackLink(any(), any())(using any())
  }

  def verifyDataCacheServiceRetursHasBeenRevalued(revalued: String) = {
    verify(mockDataCacheService).fetchAndGetData[HasBeenRevalued](
      eqs(revalued)
    )(using any(), any())
  }

  def verifyPropertyDetailsService(isPropertyRevalued: Option[Boolean],
                                   revaluedValue: Option[BigDecimal],
                                   revaluedDate: Option[LocalDate],
                                   partAcqDispDate: Option[LocalDate]) = {
    val expectedPropertyDetails = PropertyDetailsRevalued(
      isPropertyRevalued = isPropertyRevalued,
      revaluedValue = revaluedValue,
      revaluedDate = revaluedDate,
      partAcqDispDate = partAcqDispDate
    )
    verify(mockPropertyDetailsService).saveDraftPropertyDetailsRevalued(any(), eqs(expectedPropertyDetails))(using any(), any())
  }

  def beforeEach(): Unit = {
    stubServiceNavigationUrls(mockAppConfig)
  }

}
