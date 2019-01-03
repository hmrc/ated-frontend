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

package services

import java.util.UUID

import builders.{AuthBuilder, ReliefBuilder}
import connectors.{AtedConnector, DataCacheConnector}
import models.{TaxAvoidance, _}
import org.joda.time.{DateTime, LocalDate}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.AtedConstants._

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import uk.gov.hmrc.http.logging.SessionId

class ReliefsServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with PrivateMethodTester {

  import AuthBuilder._

  val mockAtedConnector = mock[AtedConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789013"

  object TestReliefsService extends ReliefsService {
    override val atedConnector = mockAtedConnector
    override val dataCacheConnector = mockDataCacheConnector
    val updateReliefsPrivate = PrivateMethod[Future[ReliefsTaxAvoidance]]('updateReliefs)
  }

  override def beforeEach = {
    reset(mockAtedConnector)
    reset(mockDataCacheConnector)
  }

  val periodKey = 2015
  implicit val user = createAtedContext(createAgentAuthContext("User-Id", "name", Some("JARN1234567")))

  "ReliefsService" must {
    "use the correct connector" in {
      ReliefsService.atedConnector must be(AtedConnector)
    }


    "save the draft reliefs" must {
      "Create a default Tax Avoidance if we don't already have one" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = false,
          propertyDeveloper = false,
          propertyTrading = false,
          lending = false,
          employeeOccupation = false,
          farmHouses = false,
          socialHousing = false, isAvoidanceScheme = Some(true))
        val taxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("avoid1"))
        val reliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val respJson = Json.parse("""{"reason": "some reason"}""")
        val responseJson = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(Matchers.eq("ATED-123"), Matchers.eq(periodKey))(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))

        val captor = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val result = TestReliefsService.saveDraftReliefs("ATED-123", periodKey, reliefs)
        val retrievedReliefs = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.reliefs must be(reliefs)

        //Check the correct object is being saved
        val savedReliefs = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(false)
      }

      "Create a default Tax Avoidance if we don't already have one and taxAvoidance is set to false" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = false,
          propertyDeveloper = false,
          propertyTrading = false,
          lending = false,
          employeeOccupation = false,
          farmHouses = false,
          socialHousing = false, isAvoidanceScheme = None)
        val taxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val respJson = Json.parse("""{"reason": "some reason"}""")
        val responseJson = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(Matchers.eq("ATED-123"), Matchers.eq(periodKey))(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))

        val captor = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val result = TestReliefsService.saveDraftReliefs("ATED-123", periodKey, reliefs)
        val retrievedReliefs = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.reliefs must be(reliefs)

        //Check the correct object is being saved
        val savedReliefs = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(false)
      }

      "overwrite the current Tax Avoidance, if the tax avoidance option isn't set" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))


        val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = false,
          propertyDeveloper = false,
          propertyTrading = false,
          lending = false,
          employeeOccupation = false,
          farmHouses = false,
          socialHousing = false, isAvoidanceScheme = Some(true))
        val taxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val responseJson = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val captor = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val result = TestReliefsService.saveDraftReliefs("ATED-123", periodKey, reliefs)
        val retrievedReliefs = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.reliefs must be(reliefs)

        //Check the correct object is being saved
        val savedReliefs = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(false)
      }

      "throw Internal server exception , for any other status code" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = false,
          propertyDeveloper = false,
          propertyTrading = false,
          lending = false,
          employeeOccupation = false,
          farmHouses = false,
          socialHousing = false, isAvoidanceScheme = Some(true))
        val taxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val respJson = Json.parse("""{"reason": "some reason"}""")
        val responseJson = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val captor = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, Some(respJson))))

        val result = TestReliefsService.saveDraftReliefs("ATED-123", periodKey, reliefs)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.message must include("status : 502")
      }

      "keep the current Tax Avoidance if the tax avoidance option is true" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = false,
          propertyDeveloper = false,
          propertyTrading = false,
          lending = false,
          employeeOccupation = false,
          farmHouses = false,
          socialHousing = false, isAvoidanceScheme = Some(true))
        val taxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("Avoid-1"))
        val reliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val responseJson = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val captor = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val result = TestReliefsService.saveDraftReliefs("ATED-123", periodKey, reliefs)
        val retrievedReliefs = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.reliefs must be(reliefs)

        //Check the correct object is being saved
        val savedReliefs = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.get must be("Avoid-1")
      }
    }

    "Save the tax avoidance" must {
      "Create a default Reliefs if we don't already have one and wipe the tax avoidance" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = false,
          openToPublic = false,
          propertyDeveloper = false,
          propertyTrading = false,
          lending = false,
          employeeOccupation = false,
          farmHouses = false,
          socialHousing = false, isAvoidanceScheme = None)
        val taxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val responseJson = Json.toJson(reliefsTaxAvoidance)
        val respJson = Json.parse("""{"reason": "some reason"}""")

        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))

        val captor = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val result = TestReliefsService.saveDraftTaxAvoidance("ATED-123", periodKey, taxAvoidance)
        val retrievedReliefs = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.taxAvoidance must be(taxAvoidance)

        //Check the correct object is being saved
        val savedReliefs = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(false)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(false)
      }

      "Keep the current Reliefs if we have one" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = false,
          propertyDeveloper = false,
          propertyTrading = false,
          lending = false,
          employeeOccupation = false,
          farmHouses = false,
          socialHousing = false, isAvoidanceScheme = Some(true))
        val taxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("Avoid-1"))
        val reliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val responseJson = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val captor = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val result = TestReliefsService.saveDraftTaxAvoidance("ATED-123", periodKey, taxAvoidance)
        val retrievedReliefs = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.taxAvoidance must be(taxAvoidance)

        //Check the correct object is being saved
        val savedReliefs = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.get must be("Avoid-1")
      }

      "throw Internal server exception , for any other status code" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = false,
          propertyDeveloper = false,
          propertyTrading = false,
          lending = false,
          employeeOccupation = false,
          farmHouses = false,
          socialHousing = false, isAvoidanceScheme = None)
        val taxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val respJson = Json.parse("""{"reason": "some reason"}""")
        val responseJson = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val captor = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, Some(respJson))))

        val result = TestReliefsService.saveDraftTaxAvoidance("ATED-123", periodKey, taxAvoidance)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.message must include("status : 502")
      }

    }

    "Save is avoidance scheme" must {
      "throw an exception if we don't have cached reliefs" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val respJson = Json.parse("""{"reason": "some reason"}""")

        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(respJson))))

        val result = TestReliefsService.saveDraftIsTaxAvoidance("ATED-123", periodKey, true)
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("[ReliefsService][updateIsTaxAvoidance] : No Draft Relief found")
      }

      "Keep the current Reliefs if we have one" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = false,
          propertyDeveloper = false,
          propertyTrading = false,
          lending = false,
          employeeOccupation = false,
          farmHouses = false,
          socialHousing = false,
          isAvoidanceScheme = Some(false))

        val reliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, TaxAvoidance())
        val responseJson = Json.toJson(reliefsTaxAvoidance)

        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val captor = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))


        val result = TestReliefsService.saveDraftIsTaxAvoidance("ATED-123", periodKey, true)
        await(result)

        //Check the correct object is being saved
        val savedReliefs = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.reliefs.isAvoidanceScheme must be(Some(true))
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(false)
      }

      "throw Internal server exception , for any other status code" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = false,
          propertyDeveloper = false,
          propertyTrading = false,
          lending = false,
          employeeOccupation = false,
          farmHouses = false,
          socialHousing = false, isAvoidanceScheme = None)
        val taxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val respJson = Json.parse("""{"reason": "some reason"}""")
        val responseJson = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))

        val captor = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, Some(respJson))))

        val result = TestReliefsService.saveDraftIsTaxAvoidance("ATED-123", periodKey, true)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.message must include("status : 502")
      }

    }
    "Retrieve the draft reliefs" must {
      "successfully retrieve the draft reliefs" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = ReliefBuilder.reliefTaxAvoidance(periodKey, Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = true,
          propertyDeveloper = true,
          propertyTrading = true,
          lending = true,
          employeeOccupation = true,
          farmHouses = true,
          socialHousing = true))

        val responseJson = Json.toJson(reliefs)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(responseJson))))
        val result = TestReliefsService.retrieveDraftReliefs("ATED-123", periodKey)
        await(result) must be(Some(reliefs))
      }

      "throw internal server exception, for any other exceptions..." in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs = ReliefBuilder.reliefTaxAvoidance(periodKey, Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = true,
          propertyDeveloper = true,
          propertyTrading = true,
          lending = true,
          employeeOccupation = true,
          farmHouses = true,
          socialHousing = true))

        val responseJson = Json.toJson(reliefs)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())
        (any(), any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
        val result = TestReliefsService.retrieveDraftReliefs("ATED-123", periodKey)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.message must include("status : 400")
      }

    }

    "Submit the draft reliefs" must {
      "successfully submit reliefs to ETMP via DES" in {

        val jsonEtmpResponse =

          """
            {
            |  "processingDate" : "2016-11-22T13:58:07.047Z",
            |  "reliefReturnResponse" : [ {
            |    "reliefDescription" : "Farmhouses",
            |    "formBundleNumber" : "123456789012"
            |  } ]
            |}
          """.stripMargin
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse = Json.parse(jsonEtmpResponse)

        when(mockAtedConnector.submitDraftReliefs(any(), any())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        when(mockDataCacheConnector.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK, None)))


        val SubmitReturnsResponseFormId = "submit-returns-response-Id"
        when(mockDataCacheConnector.saveFormData[SubmitReturnsResponse](Matchers.eq(SubmitReturnsResponseFormId),
          Matchers.eq(successResponse.as[SubmitReturnsResponse]))
          (any(), any(), Matchers.eq(SubmitReturnsResponse.formats)))
          .thenReturn(Future.successful(successResponse.as[SubmitReturnsResponse]))

        val result = TestReliefsService.submitDraftReliefs("ATED-123", periodKey)
        val response = await(result)

        verify(mockDataCacheConnector, times(1)).clearCache()(any())
        verify(mockDataCacheConnector, times(1)).saveFormData(any(),any())(any(),any(),any())
      }

    }

    "View relief return" must {
      "if summary data is found in Cache, return Some EtmpReliefReturnsSummary" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val submittedReliefReturns1 = SubmittedReliefReturns(formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
        val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), true, "payment-ref-01")
        val submittedReturns = SubmittedReturns(periodKey, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
        val periodSummaryReturns = PeriodSummaryReturns(periodKey, Seq(), Some(submittedReturns))
        val data = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))
        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId))
          (any(), any(), any())).thenReturn(Future.successful(Some(data)))
        val result = TestReliefsService.viewReliefReturn(periodKey, formBundleNo1)
        await(result) must be(Some(submittedReliefReturns1))
      }

      "if summary data is found in Cache, but relief return  is None, return None" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), true, "payment-ref-01")
        val submittedReturns = SubmittedReturns(periodKey, Seq(), Seq(submittedLiabilityReturns1))
        val periodSummaryReturns = PeriodSummaryReturns(periodKey, Seq(), Some(submittedReturns))
        val data = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))
        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId))
          (any(), any(), any())).thenReturn(Future.successful(Some(data)))
        val result = TestReliefsService.viewReliefReturn(periodKey, formBundleNo1)
        await(result) must be(None)
      }
      "if summary data is found in Cache, but it doesn't contain EtmpResponseWrapper, return None" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val formBundleNo = "form-123"
        val data = SummaryReturnsModel(None, Nil)
        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId))
          (any(), any(), any())).thenReturn(Future.successful(Some(data)))
        val result = TestReliefsService.viewReliefReturn(periodKey, formBundleNo)
        await(result) must be(None)
      }
      "if no summary data is found in Cache, return None" in {
        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val formBundleNo = "form-123"
        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId))
          (any(), any(), any())).thenReturn(Future.successful(None))
        val result = TestReliefsService.viewReliefReturn(periodKey, formBundleNo)
        await(result) must be(None)
      }
    }

    "delete relief returns" must {

      "remove the draft reliefs from the cache" in {

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        when(mockAtedConnector.deleteDraftReliefs(any(), any())) thenReturn Future.successful(HttpResponse(200, None))

        val result = TestReliefsService.clearDraftReliefs
        await(result).status must be(OK)
      }

    }

    "delete relief draft returns from period key" must {

      "delete the draft return from mongodb" in {

        implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        when(mockAtedConnector.deleteDraftReliefsByYear(any())(any(), any())) thenReturn Future.successful(HttpResponse(200, None))

        val result = TestReliefsService.deleteDraftReliefs(2017)
        await(result).status must be (OK)
      }
    }

  }

}
