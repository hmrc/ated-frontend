package controllers.propertyDetails

import builders.PropertyDetailsBuilder
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthAction
import models.{DateOfChange, HasBeenRevalued, PropertyDetailsNewValuation}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.mvc.MessagesControllerComponents
import services.{PropertyDetailsCacheSuccessResponse, PropertyDetailsService, ServiceInfoService}
import testhelpers.MockAuthUtil
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment}
import uk.gov.hmrc.http.HeaderCarrier
import utils.AtedConstants.{DelegatedClientAtedRefNumber, FortyThousandValueDateOfChange, HasPropertyBeenRevalued, propertyDetailsNewValuationValue}
import views.html.BtaNavigationLinks
import views.html.propertyDetails.propertyDetailsDateOfRevalue

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class PropertyDetailsTestFixture extends PlaySpec with GuiceOneServerPerSuite with MockAuthUtil{


  implicit val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockPropertyDetailsService: PropertyDetailsService = mock[PropertyDetailsService]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val mockBackLinkCacheConnector: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockNewValuationController: PropertyDetailsNewValuationController = mock[PropertyDetailsNewValuationController]
  val mockIsFullTaxPeriodController: IsFullTaxPeriodController = mock[IsFullTaxPeriodController]
  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy implicit val messages: MessagesImpl = MessagesImpl(Lang("en-GB"), messagesApi)
  val btaNavigationLinksView: BtaNavigationLinks = app.injector.instanceOf[BtaNavigationLinks]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val injectedViewInstance: propertyDetailsDateOfRevalue = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsDateOfRevalue]

    val mockAuthAction: AuthAction = new AuthAction(
      mockAppConfig,
      mockDelegationService,
      mockAuthConnector
    )

    val testController: PropertyDetailsDateOfRevalueController = new PropertyDetailsDateOfRevalueController(
      mockMcc,
      mockAuthAction,
      mockServiceInfoService,
      injectedViewInstance,
      mockPropertyDetailsService,
      mockBackLinkCacheConnector,
      mockDataCacheConnector,
      mockIsFullTaxPeriodController
    )

  def setupAuthForOrganisation(enrolmentSet: Set[Enrolment]) {
    val authMock = authResultDefault(AffinityGroup.Organisation, enrolmentSet)
    enrolmentSet match {
      case "invalidEnrolmentSet" => setInvalidAuthMocks(authMock)
      case "defaultEnrolmentSet" => setAuthMocks(authMock)
    }
  }

  def setupDataCacheConnectorExpectations(newValuation: Some[BigDecimal], hasPropertyBeenRevalued: Some[Boolean], dateOfRevaluationChange: Some[LocalDate]) = {
    when(mockDataCacheConnector.fetchAndGetFormData[HasBeenRevalued](ArgumentMatchers.eq(HasPropertyBeenRevalued))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(HasBeenRevalued(hasPropertyBeenRevalued))))


    when(mockDataCacheConnector.fetchAndGetFormData[PropertyDetailsNewValuation](ArgumentMatchers.eq(propertyDetailsNewValuationValue))
      (ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(PropertyDetailsNewValuation(newValuation))))

    when(mockDataCacheConnector.fetchAndGetFormData[DateOfChange](ArgumentMatchers.eq(FortyThousandValueDateOfChange))
      (ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Some(DateOfChange(dateOfRevaluationChange))))
  }
}
  def setupCommonDependencies(isFeatureFlagEnabled: Boolean): Unit = {
    when(mockAppConfig.newRevaluedFeature).thenReturn(isFeatureFlagEnabled)
    when(mockServiceInfoService.getPartial(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(btaNavigationLinksView()(messages, mockAppConfig)))
    when(mockDataCacheConnector.fetchAtedRefData[String](ArgumentMatchers.eq(DelegatedClientAtedRefNumber))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some("XN1200000100001")))
    when(mockDataCacheConnector.fetchAndGetFormData[Boolean](ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCacheConnector.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
  }

  def setupPropertyDetails(): Unit = {
    val propertyDetails = PropertyDetailsBuilder.getPropertyDetails("1", Some("z11 1zz")).copy(value = None)
    when(mockPropertyDetailsService.retrieveDraftPropertyDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(PropertyDetailsCacheSuccessResponse(propertyDetails)))
  }
    override val userId = s"user-${UUID.randomUUID}"

}
