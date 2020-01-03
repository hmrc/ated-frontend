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

package services

import connectors.{AtedConnector, DataCacheConnector}
import models.{PeriodSummaryReturns, _}
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants._

import scala.concurrent.Future

class SummaryReturnsServiceSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val periodKey: Int = 2015
  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789013"

  class Setup {
    val testSummaryReturnsService: SummaryReturnsService = new SummaryReturnsService(
      mockAtedConnector,
      mockDataCacheConnector
    )
  }

  override def beforeEach: Unit = {
    reset(mockAtedConnector)
    reset(mockDataCacheConnector)
  }

  "SummmaryReturnsService" must {
    "getSummaryReturns" must {

      val data = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(
        PeriodSummaryReturns(periodKey, Seq(
          DraftReturns(periodKey, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft),
          DraftReturns(periodKey, "", "some relief", None, TypeReliefDraft)),
          Some(
            SubmittedReturns(
              periodKey,
              Seq(
                SubmittedReliefReturns(formBundleNo1, "some relief", new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
              ),
              Seq(
                SubmittedLiabilityReturns(
                  formBundleNo2, "addr1+2", BigDecimal(1234.00), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), changeAllowed = true, "payment-ref-01"
                )
              )
            )
        ))
      ))
      val json = Json.toJson(data)
      val json2 = Json.toJson(
        SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(
          PeriodSummaryReturns(
            periodKey,
            Seq(
              DraftReturns(periodKey, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft),
              DraftReturns(periodKey, "", "some relief", None, TypeReliefDraft)
            ),
            None
          )
        ))
      )
      val json3 = Json.toJson(data.copy(allReturns = data.allReturns :+ PeriodSummaryReturns(3, Seq(), None)))

      "when 1st time this method is called, it calls ated and saves submitted returns data into cache" must {
        "data returned from cache would be None, and we call full summary return URL in ated" must {
          "connector returns OK as response, then Return SummaryReturnsModel after filtering out errant period" in new Setup {
            when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId))(any(),any(),any()))
              .thenReturn(Future.successful(None))

            when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId), any())(any(),any(),any()))
              .thenReturn(Future.successful(data))

            when(mockAtedConnector.getFullSummaryReturns(any(),any()))
              .thenReturn(Future.successful(HttpResponse(OK, Some(json3))))

            val result: Future[SummaryReturnsModel] = testSummaryReturnsService.getSummaryReturns
            await(result) must be(data)
          }

          "connector returns NON-OK as response, then throw exception" in new Setup {

            when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId))(any(),any(),any()))
              .thenReturn(Future.successful(None))

            when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId),any())(any(),any(),any()))
              .thenReturn(Future.successful(data))

            when(mockAtedConnector.getFullSummaryReturns(any(),any()))
              .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(json))))

            val result: Future[SummaryReturnsModel] = testSummaryReturnsService.getSummaryReturns
            val thrown: RuntimeException = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must include("[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - Status other than 200 returned - No Cache")
          }
        }
      }

      "when NOT 1st time this method is called, it does partial call to ated and merges cached data" must {
        "data returned from cache would be Some(SummaryReturnsModel) without any drafts, and we call partial summary return URL in ated" must {
          "connector returns OK as response, then Return SummaryReturnsModel" in new Setup {
            val dataCached: SummaryReturnsModel = data.copy(allReturns = data.allReturns.map(_.copy(draftReturns = Nil)))

            when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId))(any(),any(),any()))
              .thenReturn(Future.successful(Some(dataCached)))

            when(mockAtedConnector.getPartialSummaryReturns(any(),any()))
              .thenReturn(Future.successful(HttpResponse(OK, Some(json2))))

            val result: Future[SummaryReturnsModel] = testSummaryReturnsService.getSummaryReturns
            await(result) must be(data)

            verify(mockDataCacheConnector, times(0))
              .saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId),any())(any(),any(),any())
          }

          "connector returns NON-OK as response, then throw exception" in new Setup {
            val dataCached: SummaryReturnsModel = data.copy(allReturns = data.allReturns.map(_.copy(draftReturns = Nil)))

            when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId))(any(),any(),any()))
              .thenReturn(Future.successful(Some(dataCached)))

            when(mockAtedConnector.getPartialSummaryReturns(any(),any()))
              .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(json2))))

            val result: Future[SummaryReturnsModel] = testSummaryReturnsService.getSummaryReturns
            val thrown: RuntimeException = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must include("[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - Status other than 200 returned - Has Cache")
            verify(mockDataCacheConnector, times(0))
              .saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId),any())(any(),any(),any())
          }
        }
      }
    }

    "getPeriodSummaryReturns" must {

      val draftReturns1 = DraftReturns(periodKey, "1", "desc", Some(BigDecimal(100.00)), TypeChangeLiabilityDraft)
      val draftReturns2 = DraftReturns(periodKey, "", "some relief", None, TypeReliefDraft)
      val submittedReliefReturns1 = SubmittedReliefReturns(formBundleNo1, "some relief",
        new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"))
      val submittedLiabilityReturns1 = SubmittedLiabilityReturns(formBundleNo2, "addr1+2", BigDecimal(1234.00),
        new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), new LocalDate("2015-05-05"), changeAllowed = true, "payment-ref-01")
      val submittedReturns = SubmittedReturns(periodKey, Seq(submittedReliefReturns1), Seq(submittedLiabilityReturns1))
      val periodSummaryReturns = PeriodSummaryReturns(periodKey, Seq(draftReturns1, draftReturns2), Some(submittedReturns))
      val data = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))
      val json = Json.toJson(data)

      "return Some(PeriodSummaryReturns), if that period is found in SummaryReturnsModel" in new Setup {
        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId),any())(any(),any(),any()))
          .thenReturn(Future.successful(data))

        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId))(any(),any(),any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId),any())(any(),any(),any()))
          .thenReturn(Future.successful(data))

        when(mockAtedConnector.getFullSummaryReturns(any(),any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(json))))

        val result: Future[Option[PeriodSummaryReturns]] = testSummaryReturnsService.getPeriodSummaryReturns(periodKey)
        await(result) must be(Some(periodSummaryReturns))
      }

      "return None, if that period is not-found in SummaryReturnsModel" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId))(any(),any(),any()))
            .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId),any())(any(),any(),any()))
            .thenReturn(Future.successful(data))

        when(mockAtedConnector.getFullSummaryReturns(any(),any()))
            .thenReturn(Future.successful(HttpResponse(OK, Some(json))))

        val result: Future[Option[PeriodSummaryReturns]] = testSummaryReturnsService.getPeriodSummaryReturns(periodKey + 1)
        await(result) must be(None)
      }
    }

    "getPreviousSubmittedLiabilityDetails" must {

      val submittedLiabilityReturns1 = SubmittedLiabilityReturns(
        formBundleNo2,
        "addr1+2",
        BigDecimal(1234.00),
        new LocalDate("2015-05-05"),
        new LocalDate("2015-05-05"),
        new LocalDate("2015-05-05"),
        changeAllowed = true, "payment-ref-01"
      )
      val submittedReturns = SubmittedReturns(periodKey, reliefReturns = Nil, Seq(submittedLiabilityReturns1))
      val periodSummaryReturns = PeriodSummaryReturns(periodKey, draftReturns = Nil, Some(submittedReturns))
      val data1 = SummaryReturnsModel(Some(BigDecimal(999.99)), Seq(periodSummaryReturns))
      val json1 = Json.toJson(data1)
      val prevReturn = PreviousReturns("1 address street", "12345678")
      val pastReturnDetails = Seq(prevReturn)

      "save and return past submitted liabilities for a valid user" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[PreviousReturns](eqTo(RetrieveReturnsResponseId))(any(),any(),any()))
          .thenReturn(Future.successful(None))

        when(mockAtedConnector.getFullSummaryReturns(any(),any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(json1))))

        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId),any())(any(),any(),any()))
          .thenReturn(Future.successful(data1))

        when(mockDataCacheConnector.saveFormData[Seq[PreviousReturns]](eqTo(PreviousReturnsDetailsList),any())(any(),any(),any()))
          .thenReturn(Future.successful(pastReturnDetails))

        val result: Future[Seq[PreviousReturns]] = testSummaryReturnsService.getPreviousSubmittedLiabilityDetails(periodKey + 1)
        await(result) must be(pastReturnDetails)
      }
    }

    "retrieveCachedPreviousReturnAddressList" must {
      val prevReturn = PreviousReturns("1 address street", "12345678")
      val pastReturnDetails = Some(Seq(prevReturn))

      "retrieve cached previous returns address list" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[Seq[PreviousReturns]](eqTo(PreviousReturnsDetailsList))(any(),any(),any()))
          .thenReturn(Future.successful(pastReturnDetails))

        val result: Future[Option[Seq[PreviousReturns]]] = testSummaryReturnsService.retrieveCachedPreviousReturnAddressList
        await(result) must be(pastReturnDetails)
      }
    }
  }
}
