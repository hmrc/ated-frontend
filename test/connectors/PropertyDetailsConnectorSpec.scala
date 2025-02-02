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

package connectors

import builders.PropertyDetailsBuilder
import config.ApplicationConfig
import models._

import java.time.LocalDate
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import uk.gov.hmrc.http._

import java.net.URL
import scala.concurrent.ExecutionContext

class PropertyDetailsConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting {

  class Setup extends ConnectorTest with PropertyDetailsConnectorTestConstants {
    implicit val ec: ExecutionContext = inject[ExecutionContext]
    implicit val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val mockAppConfig: ApplicationConfig = inject[ApplicationConfig]

    lazy val periodKey = 2015
    lazy val id = "1"
    lazy val atedRefNumber = "AtedRefNumber"
    lazy val serviceURL = mockAppConfig.conf.baseUrl("ated") + "/ated/"

    lazy val drafts = "drafts"
    lazy val period = "period"

    val testPropertyDetailsConnector: PropertyDetailsConnector = new PropertyDetailsConnector(mockAppConfig, mockHttpClient)
    when(authContext.atedReferenceNumber).thenReturn(atedRefNumber)

    def createUrlFor(path: String) :String = {
      s"$serviceURL$atedRefNumber/$path/$periodKey"
    }

    def getUrlFor(path: String) :String = {
      s"$serviceURL$atedRefNumber/$path/$id"
    }

    def postUrlFor(path: String, periodKey: Int) :String = {
      s"$serviceURL$atedRefNumber/$path/$periodKey"
    }

    def postUrlFor(path: String) :String = {
      s"$serviceURL$atedRefNumber/$path/$id"
    }

    def deleteDraftUrlFor(path: String) :String = {
      s"$serviceURL$atedRefNumber/$path/drafts/$id"
    }
  }

  "PropertyDetailsConnector" must {

    "create draft property details" must {
      val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("testPostCode"))

      "call correct url and http method to save createDraftPropertyDetails" in new Setup {
        testPropertyDetailsConnector.createDraftPropertyDetails(periodKey, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(createUrlFor(createDraftPropertyDetailsURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details address ref" must {
      val propertyDetails: PropertyDetailsAddress = PropertyDetailsBuilder.getPropertyDetailsAddress(Some("testPostCode"))

      "call correct url and http method to save saveDraftPropertyDetailsAddressRef" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsAddressRef(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsAddressRefURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details Has Value Changed" must {

      "call correct url and http method to save saveDraftHasValueChanged" in new Setup {
        testPropertyDetailsConnector.saveDraftHasValueChanged(id, propertyDetails = true)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyHasValueChangedURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details title" must {
      val propertyDetails: PropertyDetailsTitle = PropertyDetailsTitle("")

      "call correct url and http method to save saveDraftPropertyDetailsTitle" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsTitle(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsTitleURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details Acquisition" must {

      "call correct url and http method to save saveDraftPropertyDetailsAcquisition" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsAcquisition(id, overLimit = true)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsAcquisitionURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details Revalued" must {
      val propertyDetails = new PropertyDetailsRevalued()

      "call correct url and http method to save saveDraftPropertyDetailsRevalued" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsRevalued(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsRevaluedURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details OwnedBefore" must {
      val propertyDetails = new PropertyDetailsOwnedBefore()

      "call correct url and http method to save saveDraftPropertyDetailsOwnedBefore" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsOwnedBefore(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsOwnedBeforeURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details ProfessionallyValued" must {
      val propertyDetails = new PropertyDetailsProfessionallyValued()

      "call correct url and http method to save saveDraftPropertyDetailsProfessionallyValued" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsProfessionallyValued(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsValuedURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details NewBuild" must {
      val propertyDetailsIsNewBuild = new PropertyDetailsNewBuild()

      "call correct url and http method to save saveDraftPropertyDetailsNewBuild" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsNewBuild(id, propertyDetailsIsNewBuild)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsNewBuildURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details new build dates" must {
      val testPropertyDetailsNewBuildDates = new PropertyDetailsNewBuildDates(None, None)

      "call correct url and http method to save saveDraftPropertyDetailNewBuildDates" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailNewBuildDates(id, testPropertyDetailsNewBuildDates)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsNewBuildDatesURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details when acquired dates" must {
      val testPropertyDetailsWhenAcquiredDates = new PropertyDetailsWhenAcquiredDates(None)

      "call correct url and http method to save saveDraftPropertyDetailsWhenAcquiredDates" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsWhenAcquiredDates(id, testPropertyDetailsWhenAcquiredDates)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsWhenAcquiredDatesURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details new build value" must {
      val testPropertyDetailsNewBuildValue = new PropertyDetailsNewBuildValue(None)

      "call correct url and http method to save saveDraftPropertyDetailsNewBuildValue" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsNewBuildValue(id, testPropertyDetailsNewBuildValue)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsNewBuildValueURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details value acquired" must {
      val testPropertyDetailsValueAcquired = new PropertyDetailsValueOnAcquisition(None)

      "call correct url and http method to save saveDraftPropertyDetailsValueAcquired" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsValueAcquired(id, testPropertyDetailsValueAcquired)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsValueOnAcquisitionURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details IsFullTaxPeriod" must {
      val propertyDetails = new IsFullTaxPeriod(false, None)

      "call correct url and http method to save saveDraftIsFullTaxPeriod" in new Setup {
        testPropertyDetailsConnector.saveDraftIsFullTaxPeriod(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsFullTaxPeriodURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details IsInRelief" must {
      val propertyDetails = new PropertyDetailsInRelief()

      "call correct url and http method to save saveDraftPropertyDetailsInRelief" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsInRelief(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsInReliefURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details TaxAvoidance Scheme" must {
      val propertyDetails = new PropertyDetailsTaxAvoidanceScheme()

      "call correct url and http method to save saveDraftPropertyDetailsTaxAvoidanceScheme" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsTaxAvoidanceScheme(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsTaxAvoidanceURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details TaxAvoidance reference" must {
      val propertyDetails = new PropertyDetailsTaxAvoidanceReferences()

      "call correct url and http method to save - saveDraftPropertyDetailsTaxAvoidanceReferences" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsTaxAvoidanceReferences(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsTaxAvoidanceURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details DatesLiable" must {
      val propertyDetails = new PropertyDetailsDatesLiable(Some(LocalDate.parse("1970-01-01")), Some(LocalDate.parse("1970-01-01")))

      "call correct url and http method to save - saveDraftPropertyDetailsDatesLiable" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsDatesLiable(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsDatesLiableURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "add DatesLiable" must {
      val propertyDetails = PropertyDetailsDatesLiable(
        Some(LocalDate.parse("2999-02-03")), Some(LocalDate.parse("2999-03-04"))
      )

      "call correct url and http method to add - addDraftPropertyDetailsDatesLiable" in new Setup {
        testPropertyDetailsConnector.addDraftPropertyDetailsDatesLiable(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(addDraftPropertyDetailsDatesLiableURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "delete Period" must {
      val propertyDetails = PropertyDetailsDatesLiable(
        Some(LocalDate.parse("2999-02-03")), Some(LocalDate.parse("2999-03-04"))
      )

      "call correct url and http method to delete - deleteDraftPropertyDetailsPeriod" in new Setup {
        testPropertyDetailsConnector.deleteDraftPropertyDetailsPeriod(id, propertyDetails.startDate.get)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(deleteDraftPropertyDetailsPeriodURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "add DatesInRelief" must {
      val propertyDetails = PropertyDetailsDatesInRelief(
        Some(LocalDate.parse("2999-02-03")), Some(LocalDate.parse("2999-03-04"))
      )

      "call correct url and http method to add - addDraftPropertyDetailsDatesInRelief" in new Setup {
        testPropertyDetailsConnector.addDraftPropertyDetailsDatesInRelief(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(addDraftPropertyDetailsDatesInReliefURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "save property details SupportingInfo" must {
      val propertyDetails = new PropertyDetailsSupportingInfo("")

      "call correct url and http method to save - saveDraftPropertyDetailsSupportingInfo" in new Setup {
        testPropertyDetailsConnector.saveDraftPropertyDetailsSupportingInfo(id, propertyDetails)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(saveDraftPropertyDetailsSupportingInfoURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "Calculate property details" must {

      "call correct url and http method to get details - calculateDraftPropertyDetails" in new Setup {
        testPropertyDetailsConnector.calculateDraftPropertyDetails(id)
        verify(mockHttpClient, times(1)).get(new URL(getUrlFor(calculateDraftPropertyDetailsURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "Calculate changed Liability" must {

      "call correct url and http method to get details - calculateDraftChangeLiability" in new Setup {
        testPropertyDetailsConnector.calculateDraftChangeLiability(id)
        verify(mockHttpClient, times(1)).get(new URL(getUrlFor(calculateDraftChangeLiabilityURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "Retrieve property details" must {

      "call correct url and http method to get details - retrieveDraftPropertyDetails" in new Setup {
        testPropertyDetailsConnector.retrieveDraftPropertyDetails(id)
        verify(mockHttpClient, times(1)).get(new URL(getUrlFor(retrieveDraftPropertyDetailsURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "submit property details" must {

      "call correct url and http method to submit draft property details - submitDraftPropertyDetails" in new Setup {
        testPropertyDetailsConnector.submitDraftPropertyDetails(id)
        verify(mockHttpClient, times(1)).post(new URL(postUrlFor(submitDraftPropertyDetailsURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }

    "delete draft chargeable return" must {

      "call correct url and http method to delete draft chargeable return - deleteDraftChargeable" in new Setup {
        testPropertyDetailsConnector.deleteDraftChargeable(id)
        verify(mockHttpClient, times(1)).delete(new URL(deleteDraftUrlFor(deletePropertyDetailsURI)))(hc)
        verifyNoMoreInteractions(mockHttpClient)
      }
    }
  }
}
