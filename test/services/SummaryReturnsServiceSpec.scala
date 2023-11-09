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

import config.ApplicationConfig
import connectors.{AtedConnector, DataCacheConnector}
import models.{PeriodSummaryReturns, _}
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsArray, JsObject, Json, __}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants._
import utils.TestModels
import play.api.test.Injecting

import scala.concurrent.{ExecutionContext, Future}

class SummaryReturnsServiceSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with TestModels with GuiceOneServerPerSuite with Injecting {

  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  implicit val ec: ExecutionContext = inject[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val allSummaryReturns: SummaryReturnsModel = summaryReturnsModel(periodKey = currentTaxYear)
  val allSummaryReturnsJson: JsObject = allReturnsJson()

  class Setup {
    val testSummaryReturnsService: SummaryReturnsService = new SummaryReturnsService(
      mockAtedConnector,
      mockDataCacheConnector
    )
  }

  override def beforeEach(): Unit = {
    reset(mockAtedConnector)
    reset(mockDataCacheConnector)
  }

  "SummmaryReturnsService" must {
    "getSummaryReturns" must {

      val errantPeriod = Json.arr(
        Json.obj(
          "periodKey" -> 3,
          "draftReturns" -> Json.arr()
        )
      )

      val jsonTransformer = (__ \ "allReturns").json.update(
        __.read[JsArray].map { o => o ++ errantPeriod }
      )

      val jsonWithErrantReturnPeriod = allReturnsJson().transform(jsonTransformer)

      "when 1st time this method is called, it calls ated and saves submitted returns data into cache" must {
        "data returned from cache would be None, and we call full summary return URL in ated" must {
          "connector returns OK as response, then Return SummaryReturnsModel after filtering out errant period" in new Setup {
            when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId))(any(), any()))
              .thenReturn(Future.successful(None))

            when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId), any())(any(), any()))
              .thenReturn(Future.successful(allSummaryReturns))

            when(mockAtedConnector.getFullSummaryReturns(any(), any()))
              .thenReturn(Future.successful(HttpResponse(OK, jsonWithErrantReturnPeriod.get.toString)))

            val result: Future[SummaryReturnsModel] = testSummaryReturnsService.getSummaryReturns
            await(result) must be(allSummaryReturns)
          }
        }
      }

      "when NOT 1st time this method is called, it does partial call to ated and merges cached data" must {
        "data returned from cache would be Some(SummaryReturnsModel) without any drafts, and we call partial summary return URL in ated" must {
          "connector returns OK as response, then Return SummaryReturnsModel" in new Setup {

            val dataCached: SummaryReturnsModel = summaryReturnsModel(periodKey = currentTaxYear, withDraftReturns = false)
            val fullData: SummaryReturnsModel = allSummaryReturns
            val partialDataReturned: JsObject = allReturnsJson(withSubmittedReturns = false)

            when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](
              eqTo(RetrieveReturnsResponseId))(any(), any()))
              .thenReturn(Future.successful(Some(dataCached)))

            when(mockAtedConnector.getPartialSummaryReturns(any(), any()))
              .thenReturn(Future.successful(HttpResponse(OK, partialDataReturned.toString)))

            val result: Future[SummaryReturnsModel] = testSummaryReturnsService.getSummaryReturns

            await(result) must be(fullData)

            verify(mockDataCacheConnector, times(0))
              .saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId), any())(any(), any())
          }

          "connector returns NON-OK as response, then throw exception" in new Setup {

            val dataCached: SummaryReturnsModel = summaryReturnsModel(periodKey = currentTaxYear, withDraftReturns = false)

            when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId))(any(), any()))
              .thenReturn(Future.successful(Some(dataCached)))

            when(mockAtedConnector.getPartialSummaryReturns(any(), any()))
              .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, allReturnsJson().toString)))

            val result: Future[SummaryReturnsModel] = testSummaryReturnsService.getSummaryReturns
            val thrown: RuntimeException = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must include("[SummaryReturnsService][getDraftWithEtmpSummaryReturns] - Status other than 200 returned - Has Cache")
            verify(mockDataCacheConnector, times(0))
              .saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId), any())(any(), any())
          }
        }
      }
    }

    "getPeriodSummaryReturns" must {

      "return Some(PeriodSummaryReturns), if that period is found in SummaryReturnsModel" in new Setup {

        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId), any())(any(), any()))
          .thenReturn(Future.successful(allSummaryReturns))

        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId))(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId), any())(any(), any()))
          .thenReturn(Future.successful(allSummaryReturns))

        when(mockAtedConnector.getFullSummaryReturns(any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, allSummaryReturnsJson.toString)))

        val result: Future[Option[PeriodSummaryReturns]] = testSummaryReturnsService.getPeriodSummaryReturns(currentTaxYear)
        await(result) must be(Some(allSummaryReturns.returnsCurrentTaxYear.head))
      }

      "return None, if that period is not-found in SummaryReturnsModel" in new Setup {

        when(mockDataCacheConnector.fetchAndGetFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId))(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId), any())(any(), any()))
          .thenReturn(Future.successful(allSummaryReturns))

        when(mockAtedConnector.getFullSummaryReturns(any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, allSummaryReturnsJson.toString)))

        val result: Future[Option[PeriodSummaryReturns]] = testSummaryReturnsService.getPeriodSummaryReturns(currentTaxYear + 1)
        await(result) must be(None)
      }
    }

    "getPreviousSubmittedLiabilityDetails" must {

      "save and return past submitted liabilities for a valid user" in new Setup {

        when(mockDataCacheConnector.fetchAndGetFormData[PreviousReturns](eqTo(RetrieveReturnsResponseId))(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockAtedConnector.getFullSummaryReturns(any(), any()))
          .thenReturn(Future.successful(HttpResponse(OK, allSummaryReturnsJson.toString)))

        when(mockDataCacheConnector.saveFormData[SummaryReturnsModel](eqTo(RetrieveReturnsResponseId), any())(any(), any()))
          .thenReturn(Future.successful(allSummaryReturns))

        when(mockDataCacheConnector.saveFormData[Seq[PreviousReturns]](eqTo(PreviousReturnsDetailsList), any())(any(), any()))
          .thenReturn(Future.successful(pastReturnDetails))

        val result: Future[Seq[PreviousReturns]] = testSummaryReturnsService.getPreviousSubmittedLiabilityDetails(currentTaxYear + 1)
        await(result) must be(pastReturnDetails)
      }
    }

    "retrieveCachedPreviousReturnAddressList" must {
      val prevReturn = PreviousReturns("1 address street", "12345678", new LocalDate("2015-04-02"), true)
      val pastReturnDetails = Some(Seq(prevReturn))

      "retrieve cached previous returns address list" in new Setup {
        when(mockDataCacheConnector.fetchAndGetFormData[Seq[PreviousReturns]](eqTo(PreviousReturnsDetailsList))(any(), any()))
          .thenReturn(Future.successful(pastReturnDetails))

        val result: Future[Option[Seq[PreviousReturns]]] = testSummaryReturnsService.retrieveCachedPreviousReturnAddressList
        await(result) must be(pastReturnDetails)
      }
    }

    "generateCurrentTaxYearReturns" must {
      "return no reliefs when maximum of 5 returns used up by draft and currentLiability (total is 7)" +
        " and recognise that there are no past returns" in new Setup {
        val currentYearReturns: Seq[PeriodSummaryReturns] = Seq(periodSummaryReturns(currentTaxYear,
          allDraftTypes = true, withPastReturns = true))

        val result = testSummaryReturnsService.generateCurrentTaxYearReturns(currentYearReturns)

        await(result) must be(Tuple3(
          List(
            AccountSummaryRowModel(returnType = "ated.draft", description = "desc",
              route = s"/ated/period-summary/$currentTaxYear/view-chargeable-edit/1"),
            AccountSummaryRowModel(returnType = "ated.draft", description = "some relief",
              route = s"/ated/period-summary/$currentTaxYear/view-return"),
            AccountSummaryRowModel(returnType = "ated.draft", description = "liability draft",
              route = s"/ated/period-summary/$currentTaxYear/view-chargeable/1"),
            AccountSummaryRowModel(returnType = "ated.draft", description = "dispose liability draft",
              route = s"/ated/period-summary/$currentTaxYear/view-disposal/"),
            AccountSummaryRowModel(formBundleNo = Some("123456789013"), returnType = "ated.submitted",
              description = "addr1+2", route = s"/ated/form-bundle/123456789013/$currentTaxYear")),
          6, true))
      }

      "return all returns for a period in order of draft, currentLiability and relief" +
        " and recognise that there are past returns" in new Setup {
        val currentYearReturns: Seq[PeriodSummaryReturns] =
          Seq(periodSummaryReturns(currentTaxYear))

        val result = testSummaryReturnsService.generateCurrentTaxYearReturns(currentYearReturns)

        await(result) must be(Tuple3(
          List(
            AccountSummaryRowModel(returnType = "ated.draft", description = "desc",
              route = s"/ated/period-summary/$currentTaxYear/view-chargeable-edit/1"),
            AccountSummaryRowModel(returnType = "ated.draft", description = "some relief",
              route = s"/ated/period-summary/$currentTaxYear/view-return"),
            AccountSummaryRowModel(formBundleNo = Some("123456789013"), returnType = "ated.submitted",
              description = "addr1+2", route = s"/ated/form-bundle/123456789013/$currentTaxYear"),
            AccountSummaryRowModel(
              formBundleNo = Some("123456789012"), returnType = "ated.submitted",
              description = "some relief", route = s"/ated/view-relief-return/$currentTaxYear/123456789012")),
          4, false))
      }
    }

    "filterPeriodSummaryReturnReliefs" must {
      "filter out unwanted reliefs in the past" in new Setup {
        val newerType1Return = SubmittedReliefReturns("no1", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(1))
        val olderType1Return = SubmittedReliefReturns("no2", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(2))
        val older2Type1Return = SubmittedReliefReturns("no3", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(3))

        val submittedReturns = SubmittedReturns(
          2018, Seq(newerType1Return, older2Type1Return, olderType1Return)
        )
        val periodSummaryReturns = PeriodSummaryReturns(
          2018,
          Nil,
          Some(submittedReturns)
        )

        val result = testSummaryReturnsService.filterPeriodSummaryReturnReliefs(periodSummaryReturns, true)

        result.submittedReturns.get.reliefReturns.contains(olderType1Return) mustBe true
        result.submittedReturns.get.reliefReturns.contains(older2Type1Return) mustBe true
      }

      "filter out unwanted reliefs for the current reliefs" in new Setup {
        val newerType1Return = SubmittedReliefReturns("no1", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(1))
        val olderType1Return = SubmittedReliefReturns("no2", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(2))
        val older2Type1Return = SubmittedReliefReturns("no3", "type 1", LocalDate.now(), LocalDate.now(), LocalDate.now().minusDays(3))

        val submittedReturns = SubmittedReturns(
          2018, Seq(newerType1Return, older2Type1Return, olderType1Return)
        )
        val periodSummaryReturns = PeriodSummaryReturns(
          2018,
          Nil,
          Some(submittedReturns)
        )

        val result = testSummaryReturnsService.filterPeriodSummaryReturnReliefs(periodSummaryReturns, false)

        result.submittedReturns.get.reliefReturns.contains(newerType1Return) mustBe true
      }
    }
  }
}
