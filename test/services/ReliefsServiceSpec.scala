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

package services

import java.util.UUID
import builders.ReliefBuilder
import config.ApplicationConfig
import connectors.{AtedConnector, DataCacheConnector}
import models.{TaxAvoidance, _}
import java.time.LocalDate
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.PrivateMethodTester
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import utils.AtedConstants._
import play.api.test.Injecting

import scala.concurrent.{ExecutionContext, Future}

class ReliefsServiceSpec extends PlaySpec with MockitoSugar with PrivateMethodTester with GuiceOneAppPerSuite with Injecting {

  implicit val ec: ExecutionContext = inject[ExecutionContext]
  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  implicit  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val formBundleNo1: String = "123456789012"
  val periodKey: Int = 2015
  val formBundleNo2: String = "123456789013"

  class Setup {
   val testReliefsService: ReliefsService = new ReliefsService(mockAtedConnector, mockDataCacheConnector) {
   }
 }

  "ReliefsService" must {
    "save the draft reliefs" must {
      "Create a default Tax Avoidance if we don't already have one" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true))
        val taxAvoidance: TaxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("avoid1"))
        val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val respJson: JsValue = Json.parse("""{"reason": "some reason"}""")
        val responseJson: JsValue = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(ArgumentMatchers.eq("ATED-123"), ArgumentMatchers.eq(periodKey))(any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, respJson.toString)))

        val captor: ArgumentCaptor[ReliefsTaxAvoidance] = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftReliefs("ATED-123", periodKey, reliefs)
        val retrievedReliefs: Option[ReliefsTaxAvoidance] = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.reliefs must be(reliefs)

        val savedReliefs: ReliefsTaxAvoidance = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(false)
      }

      "Create a default Tax Avoidance if we don't already have one and taxAvoidance is set to false" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = None)
        val taxAvoidance: TaxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val respJson: JsValue = Json.parse("""{"reason": "some reason"}""")
        val responseJson: JsValue = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(ArgumentMatchers.eq("ATED-123"), ArgumentMatchers.eq(periodKey))(any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, respJson.toString)))

        val captor: ArgumentCaptor[ReliefsTaxAvoidance] = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftReliefs("ATED-123", periodKey, reliefs)
        val retrievedReliefs: Option[ReliefsTaxAvoidance] = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.reliefs must be(reliefs)

        //Check the correct object is being saved
        val savedReliefs: ReliefsTaxAvoidance = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(false)
      }

      "overwrite the current Tax Avoidance, if the tax avoidance option isn't set" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true))
        val taxAvoidance: TaxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val responseJson: JsValue = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val captor: ArgumentCaptor[ReliefsTaxAvoidance] = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftReliefs("ATED-123", periodKey, reliefs)
        val retrievedReliefs: Option[ReliefsTaxAvoidance] = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.reliefs must be(reliefs)

        //Check the correct object is being saved
        val savedReliefs: ReliefsTaxAvoidance = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(false)
      }

      "throw Internal server exception , for any other status code" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true))
        val taxAvoidance: TaxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val respJson: JsValue = Json.parse("""{"reason": "some reason"}""")
        val responseJson: JsValue = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val captor: ArgumentCaptor[ReliefsTaxAvoidance] = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, respJson.toString)))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftReliefs("ATED-123", periodKey, reliefs)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.message must include("status : 502")
      }

      "keep the current Tax Avoidance if the tax avoidance option is true" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true))
        val taxAvoidance: TaxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("Avoid-1"))
        val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val responseJson: JsValue = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val captor: ArgumentCaptor[ReliefsTaxAvoidance] = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftReliefs("ATED-123", periodKey, reliefs)
        val retrievedReliefs: Option[ReliefsTaxAvoidance] = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.reliefs must be(reliefs)

        //Check the correct object is being saved
        val savedReliefs: ReliefsTaxAvoidance = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.get must be("Avoid-1")
      }
    }

    "Save the tax avoidance" must {
      "Create a default Reliefs if we don't already have one and wipe the tax avoidance" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: Reliefs = Reliefs(periodKey = periodKey, isAvoidanceScheme = None)
        val taxAvoidance: TaxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val responseJson: JsValue = Json.toJson(reliefsTaxAvoidance)
        val respJson: JsValue = Json.parse("""{"reason": "some reason"}""")

        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, respJson.toString)))

        val captor: ArgumentCaptor[ReliefsTaxAvoidance] = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftTaxAvoidance("ATED-123", periodKey, taxAvoidance)
        val retrievedReliefs: Option[ReliefsTaxAvoidance] = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.taxAvoidance must be(taxAvoidance)

        //Check the correct object is being saved
        val savedReliefs: ReliefsTaxAvoidance = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(false)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(false)
      }

      "Keep the current Reliefs if we have one" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(true))
        val taxAvoidance: TaxAvoidance = TaxAvoidance(rentalBusinessScheme = Some("Avoid-1"))
        val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val responseJson: JsValue = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val captor: ArgumentCaptor[ReliefsTaxAvoidance] = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftTaxAvoidance("ATED-123", periodKey, taxAvoidance)
        val retrievedReliefs: Option[ReliefsTaxAvoidance] = await(result)
        retrievedReliefs.isDefined must be(true)
        retrievedReliefs.get.taxAvoidance must be(taxAvoidance)

        //Check the correct object is being saved
        val savedReliefs: ReliefsTaxAvoidance = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(true)
        savedReliefs.taxAvoidance.rentalBusinessScheme.get must be("Avoid-1")
      }

      "throw Internal server exception , for any other status code" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = None)
        val taxAvoidance: TaxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val respJson: JsValue = Json.parse("""{"reason": "some reason"}""")
        val responseJson: JsValue = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val captor: ArgumentCaptor[ReliefsTaxAvoidance] = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, respJson.toString)))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftTaxAvoidance("ATED-123", periodKey, taxAvoidance)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.message must include("status : 502")
      }

    }

    "Save is avoidance scheme" must {
      "throw an exception if we don't have cached reliefs" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val respJson: JsValue = Json.parse("""{"reason": "some reason"}""")

        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, respJson.toString)))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftIsTaxAvoidance("ATED-123", periodKey, isAvoidanceScheme = true)
        val thrown: RuntimeException = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("[ReliefsService][updateIsTaxAvoidance] : No Draft Relief found")
      }

      "Keep the current Reliefs if we have one" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = Some(false))

        val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, TaxAvoidance())
        val responseJson: JsValue = Json.toJson(reliefsTaxAvoidance)

        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val captor: ArgumentCaptor[ReliefsTaxAvoidance] = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))


        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftIsTaxAvoidance("ATED-123", periodKey, isAvoidanceScheme = true)
        await(result)

        //Check the correct object is being saved
        val savedReliefs: ReliefsTaxAvoidance = captor.getValue
        savedReliefs.reliefs.rentalBusiness must be(true)
        savedReliefs.reliefs.isAvoidanceScheme must be(Some(true))
        savedReliefs.taxAvoidance.rentalBusinessScheme.isDefined must be(false)
      }

      "throw Internal server exception , for any other status code" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: Reliefs = Reliefs(periodKey = periodKey, rentalBusiness = true, isAvoidanceScheme = None)
        val taxAvoidance: TaxAvoidance = TaxAvoidance()
        val reliefsTaxAvoidance: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, reliefs, taxAvoidance)

        val respJson: JsValue = Json.parse("""{"reason": "some reason"}""")
        val responseJson: JsValue = Json.toJson(reliefsTaxAvoidance)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val captor: ArgumentCaptor[ReliefsTaxAvoidance] = ArgumentCaptor.forClass(classOf[ReliefsTaxAvoidance])
        when(mockAtedConnector.saveDraftReliefs(any(), captor.capture())
        (any(), any())).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, respJson.toString)))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.saveDraftIsTaxAvoidance("ATED-123", periodKey, isAvoidanceScheme = true)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.message must include("status : 502")
      }

    }
    "Retrieve the draft reliefs" must {
      "successfully retrieve the draft reliefs" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        val reliefs: ReliefsTaxAvoidance = ReliefBuilder.reliefTaxAvoidance(periodKey, Reliefs(periodKey = periodKey, rentalBusiness = true,
          openToPublic = true,
          propertyDeveloper = true,
          propertyTrading = true,
          lending = true,
          employeeOccupation = true,
          farmHouses = true,
          socialHousing = true))

        val responseJson: JsValue = Json.toJson(reliefs)
        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))
        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.retrieveDraftReliefs("ATED-123", periodKey)
        await(result) must be(Some(reliefs))
      }

      "take no action, for any Not Found exception" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        when(mockAtedConnector.retrievePeriodDraftReliefs(any(), any())
        (any(), any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

        val result: Future[Option[ReliefsTaxAvoidance]] = testReliefsService.retrieveDraftReliefs("ATED-123", periodKey)
        await(result) must be (None)
      }
    }

    "Submit the draft reliefs" must {
      "successfully submit reliefs to ETMP via DES" in new Setup {

        val jsonEtmpResponse: String =

          """
            {
            |  "processingDate" : "2016-11-22T13:58:07.047Z",
            |  "reliefReturnResponse" : [ {
            |    "reliefDescription" : "Farmhouses",
            |    "formBundleNumber" : "123456789012"
            |  } ]
            |}
          """.stripMargin
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val successResponse: JsValue = Json.parse(jsonEtmpResponse)

        when(mockAtedConnector.submitDraftReliefs(any(), any())
        (any(), any())).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        when(mockDataCacheConnector.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK, "")))

        val SubmitReturnsResponseFormId = "submit-returns-response-Id"
        when(mockDataCacheConnector.saveFormData[SubmitReturnsResponse](ArgumentMatchers.eq(SubmitReturnsResponseFormId),
          ArgumentMatchers.eq(successResponse.as[SubmitReturnsResponse]))
          (any(), any()))
          .thenReturn(Future.successful(successResponse.as[SubmitReturnsResponse]))

        val result: Future[HttpResponse] = testReliefsService.submitDraftReliefs("ATED-123", periodKey)
        val response: HttpResponse = await(result)

        verify(mockDataCacheConnector, times(1)).clearCache()(any())
        verify(mockDataCacheConnector, times(1)).saveFormData(any(),any())(any(),any())
        response.status must be (OK)
        response.body must include ("processingDate")
        response.body must include ("reliefReturnResponse")
      }

      "Return 404 when there are no reliefs to submit" in new Setup{
        val jsonEtmpNotFoundResponse: String =
          """
            |{
            |"reason" : "No Reliefs to submit"
            |}
          """.stripMargin
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val notFoundResponse: JsValue = Json.parse(jsonEtmpNotFoundResponse)

        when(mockAtedConnector.submitDraftReliefs(any(), any())
        (any(), any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, notFoundResponse.toString)))

        when(mockDataCacheConnector.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK, "")))

        val AlreadySubmittedReturnsResponseFormId = "already-submitted-returns-response-Id"
        when(mockDataCacheConnector.saveFormData[AlreadySubmittedReturnsResponse](ArgumentMatchers.eq(AlreadySubmittedReturnsResponseFormId),
          ArgumentMatchers.eq(notFoundResponse.as[AlreadySubmittedReturnsResponse]))
          (any(), any()))
          .thenReturn(Future.successful(notFoundResponse.as[AlreadySubmittedReturnsResponse]))

        val result: Future[HttpResponse] = testReliefsService.submitDraftReliefs("ATED-123", periodKey)
        val response: HttpResponse = await(result)

        //1 invocation for succssful submission test and 2nd invocation for 404 test
        verify(mockDataCacheConnector, times(2)).clearCache()(any())
        verify(mockDataCacheConnector, times(2)).saveFormData(any(),any())(any(),any())
        response.status must be (NOT_FOUND)
        response.body must include ("No Reliefs to submit")
      }

      "throw Internal Server Error for any other status code" in new Setup {
        val jsonEtmpBadRequestResponse: String =
          """
            |{
            |}
          """.stripMargin
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val badRequestResponse: JsValue = Json.parse(jsonEtmpBadRequestResponse)

        when(mockAtedConnector.submitDraftReliefs(any(), any())
        (any(), any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, badRequestResponse.toString)))

        when(mockDataCacheConnector.clearCache()(any())).thenReturn(Future.successful(HttpResponse(OK, "")))

        val result: Future[HttpResponse] = testReliefsService.submitDraftReliefs("ATED-123", periodKey)

        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.message must include("[ReliefsService][submitDraftReliefs] - status : 400")
      }
    }

    "View relief return" must {
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val submittedReliefReturns1: SubmittedReliefReturns = SubmittedReliefReturns(formBundleNo1, "some relief", LocalDate.parse("2015-05-05"),
        LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"))
      val submittedLiabilityReturns1: SubmittedLiabilityReturns =
        SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), LocalDate.parse("2015-05-05"),
          LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"), changeAllowed = true, "payment-ref-01")
      val submittedReturns: SubmittedReturns = SubmittedReturns(periodKey, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
      val periodSummaryReturns: PeriodSummaryReturns = PeriodSummaryReturns(periodKey, Seq(), Some(submittedReturns))
      val data: SummaryReturnsModel = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))

      "if summary data is found in Cache, return Some EtmpReliefReturnsSummary" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](ArgumentMatchers.eq(RetrieveReturnsResponseId))(any(), any()))
          .thenReturn(Future.successful(Some(data)))

        val result: Future[(Option[SubmittedReliefReturns], Boolean)] = testReliefsService.viewReliefReturn(periodKey, formBundleNo1)
        await(result) must be(Tuple2(Some(submittedReliefReturns1), true))
      }

      "if summary data is found in Cache, but relief return  is None, return None" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val submittedLiabilityReturns1: SubmittedLiabilityReturns =
          SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), LocalDate.parse("2015-05-05"),
          LocalDate.parse("2015-05-05"), LocalDate.parse("2015-05-05"), changeAllowed = true, "payment-ref-01")
        val submittedReturns: SubmittedReturns = SubmittedReturns(periodKey, Seq(), Seq(submittedLiabilityReturns1))
        val periodSummaryReturns: PeriodSummaryReturns = PeriodSummaryReturns(periodKey, Seq(), Some(submittedReturns))
        val data: SummaryReturnsModel = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))

        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](ArgumentMatchers.eq(RetrieveReturnsResponseId))(any(), any()))
          .thenReturn(Future.successful(Some(data)))

        val result: Future[(Option[SubmittedReliefReturns], Boolean)] = testReliefsService.viewReliefReturn(periodKey, formBundleNo1)
        await(result) must be(Tuple2(None, false))
      }
      "if summary data is found in Cache, but it doesn't contain EtmpResponseWrapper, return None" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val formBundleNo = "form-123"
        val data: SummaryReturnsModel = SummaryReturnsModel(None, Nil)

        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](ArgumentMatchers.eq(RetrieveReturnsResponseId))(any(), any()))
          .thenReturn(Future.successful(Some(data)))

        val result: Future[(Option[SubmittedReliefReturns], Boolean)] = testReliefsService.viewReliefReturn(periodKey, formBundleNo)
        await(result) must be(Tuple2(None, false))
      }
      "if no summary data is found in Cache, return None" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val formBundleNo = "form-123"

        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](ArgumentMatchers.eq(RetrieveReturnsResponseId))(any(), any()))
          .thenReturn(Future.successful(None))

        val result: Future[(Option[SubmittedReliefReturns], Boolean)] = testReliefsService.viewReliefReturn(periodKey, formBundleNo)
        await(result) must be(Tuple2(None, false))
      }
    }

    "delete relief draft returns from period key" must {

      "delete the draft return from mongodb" in new Setup {
        val periodKey = 2017
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        when(mockAtedConnector.deleteDraftReliefsByYear(any())(any(), any())) thenReturn Future.successful(HttpResponse(OK, ""))

        val result: Future[HttpResponse] = testReliefsService.deleteDraftReliefs(periodKey)
        await(result).status must be (OK)
      }
    }

  }

}
