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

package services

import builders.AuthBuilder
import connectors.{AtedConnector, DataCacheConnector}
import models.{PeriodSummaryReturns, _}
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants._

import scala.concurrent.Future

class SummaryReturnsServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  import AuthBuilder._

  val mockAtedConnector = mock[AtedConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val periodKey = 2015
  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789013"

  object TestSummaryReturnsService extends SummaryReturnsService {
    override val atedConnector = mockAtedConnector
    override val dataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach = {
    reset(mockAtedConnector)
    reset(mockDataCacheConnector)
  }

  implicit val user = createAtedContext(createUserAuthContext("User-Id", "name"))
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "SummmaryReturnsService" must {

    "use the correct connectors" in {
      SummaryReturnsService.atedConnector must be(AtedConnector)
      SummaryReturnsService.dataCacheConnector must be(DataCacheConnector)
    }

    "getSummaryReturns" must {

      val draftReturns1 = DraftReturns(2015, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
      val draftReturns2 = DraftReturns(2015, "", "some relief", None, TypeReliefDraft)
      val submittedReliefReturns1 = SubmittedReliefReturns(formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
      val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), true, "payment-ref-01")
      val submittedReturns = SubmittedReturns(2015, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
      val periodSummaryReturns = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
      val periodSummaryReturns2 = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), None)
      val data = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))
      val data2 = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns2))
      val data3 = data.copy(allReturns = data.allReturns :+ PeriodSummaryReturns(20, Seq(), None))
      val json = Json.toJson(data)
      val json2 = Json.toJson(data2)
      val json3 = Json.toJson(data3)

      "when 1st time this method is called, it calls ated and saves submitted returns data into cache" must {

        "data returned from cache would be None, and we call full summary return URL in ated" must {

          "connector returns OK as response, then Return SummaryReturnsModel after filtering out errant period" in {

            when(mockDataCacheConnector.fetchClientData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId))(Matchers.any(), Matchers.any(), Matchers.any()))
              .thenReturn(Future.successful(None))
            when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
              .thenReturn(Future.successful(data))
            when(mockAtedConnector.getFullSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(json3))))
            val result = TestSummaryReturnsService.getSummaryReturns
            await(result) must be(data)
          }

          "connector returns NON-OK as response, then throw exception" in {

            when(mockDataCacheConnector.fetchClientData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId))(Matchers.any(), Matchers.any(), Matchers.any()))
              .thenReturn(Future.successful(None))
            when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
              .thenReturn(Future.successful(data))
            when(mockAtedConnector.getFullSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(json))))
            val result = TestSummaryReturnsService.getSummaryReturns
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must include("[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - Status other than 200 returned - No Cache")
          }
        }
      }

      "when NOT 1st time this method is called, it does partial call to ated and merges cached data" must {

        "data returned from cache would be Some(SummaryReturnsModel) without any drafts, and we call partial summary return URL in ated" must {

          "connector returns OK as response, then Return SummaryReturnsModel" in {

            val dataCached = data.copy(allReturns = data.allReturns.map(_.copy(draftReturns = Nil)))
            when(mockDataCacheConnector.fetchClientData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId))(Matchers.any(), Matchers.any(), Matchers.any()))
              .thenReturn(Future.successful(Some(dataCached)))
            when(mockAtedConnector.getPartialSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(json2))))
            val result = TestSummaryReturnsService.getSummaryReturns
            await(result) must be(data)
            verify(mockDataCacheConnector, times(0)).saveFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
          }


          "connector returns NON-OK as response, then throw exception" in {

            val dataCached = data.copy(allReturns = data.allReturns.map(_.copy(draftReturns = Nil)))
            when(mockDataCacheConnector.fetchClientData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId))(Matchers.any(), Matchers.any(), Matchers.any()))
              .thenReturn(Future.successful(Some(dataCached)))
            when(mockAtedConnector.getPartialSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(json2))))
            val result = TestSummaryReturnsService.getSummaryReturns
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must include("[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - Status other than 200 returned - Has Cache")
            verify(mockDataCacheConnector, times(0)).saveFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
          }
        }
      }

    }

    "getPeriodSummaryReturns" must {

      val draftReturns1 = DraftReturns(2015, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
      val draftReturns2 = DraftReturns(2015, "", "some relief", None, TypeReliefDraft)
      val submittedReliefReturns1 = SubmittedReliefReturns(formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
      val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), true, "payment-ref-01")
      val submittedReturns = SubmittedReturns(2015, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
      val periodSummaryReturns = PeriodSummaryReturns(2015, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
      val data = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))
      val json = Json.toJson(data)

      "return Some(PeriodSummaryReturns), if that period is found in SummaryReturnsModel" in {
        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(data))
        when(mockDataCacheConnector.fetchClientData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId))(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(None))
        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(data))
        when(mockAtedConnector.getFullSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(json))))
        val result = TestSummaryReturnsService.getPeriodSummaryReturns(periodKey)
        await(result) must be(Some(periodSummaryReturns))
      }

      "return None, if that period is not-found in SummaryReturnsModel" in {
        when(mockDataCacheConnector.fetchClientData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId))(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(None))
        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(data))
        when(mockAtedConnector.getFullSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(json))))
        val result = TestSummaryReturnsService.getPeriodSummaryReturns(periodKey+1)
        await(result) must be(None)
      }
    }

    "getPreviousSubmittedLiabilityDetails" must {

      val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), true, "payment-ref-01")
      val submittedReturns = SubmittedReturns(2015, reliefReturns = Nil, Seq(submittedLiabilityReturns1))
      val periodSummaryReturns = PeriodSummaryReturns(2015, draftReturns = Nil, Some(submittedReturns))
      val periodSummaryReturns2 = PeriodSummaryReturns(2015, draftReturns = Nil, None)
      val data1 = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))
      val json1 = Json.toJson(data1)
      val data2 = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns2))
      val json2 = Json.toJson(data2)
      val prevReturn = PreviousReturns("1 address street", "12345678")
      val pastReturnDetails = Seq(prevReturn)

      "save and return past submitted liabilities for a valid user" in {
        when(mockDataCacheConnector.fetchClientData[PreviousReturns](Matchers.eq(RetrieveReturnsResponseId))(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(None))
        when(mockAtedConnector.getFullSummaryReturns(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(json1))))
        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](Matchers.eq(RetrieveReturnsResponseId), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(data1))

        when(mockDataCacheConnector.saveFormData[Seq[PreviousReturns]](Matchers.eq(PreviousReturnsDetailsList), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(pastReturnDetails))

        val result = TestSummaryReturnsService.getPreviousSubmittedLiabilityDetails
        await(result) must be(pastReturnDetails)
      }
    }

    "retrieveCachedPreviousReturnAddressList" must {
      val prevReturn = PreviousReturns("1 address street", "12345678")
      val pastReturnDetails = Some(Seq(prevReturn))

      "retrieve cached previous returns address list" in {
        when(mockDataCacheConnector.fetchAndGetFormData[Seq[PreviousReturns]](Matchers.eq(PreviousReturnsDetailsList))(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(pastReturnDetails))

        val result = TestSummaryReturnsService.retrieveCachedPreviousReturnAddressList
        await(result) must be(pastReturnDetails)

      }

    }


  }

}
