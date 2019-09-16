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

import builders.DisposeLiabilityReturnBuilder
import connectors.{AtedConnector, DataCacheConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants._
import utils.MockAuthUtil

import scala.concurrent.Future

class DisposeLiabilityReturnServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789000"
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  val periodKey = 2015

  object TestDisposeLiabilityReturnService extends DisposeLiabilityReturnService {
    override val atedConnector: AtedConnector = mockAtedConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach: Unit = {
    reset(mockAtedConnector)
    reset(mockDataCacheConnector)
  }

  val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("123456789012")
  val disposeLiabilityReturnJson: JsValue = Json.toJson(disposeLiabilityReturn)

  val updatedDate: DisposeLiability = DisposeLiabilityReturnBuilder.generateDisposalDate(periodKey)
  val bankDetails1: BankDetails = BankDetails()

  "DisposeLiabilityReturnService" must {

    "use correct connectors" in {
      DisposeLiabilityReturnService.atedConnector must be(AtedConnector)
      DisposeLiabilityReturnService.dataCacheConnector must be(DataCacheConnector)
    }

    "retrieveLiabilityReturn" must {

      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is found in cache/ETMP, i.e. for status-code OK" in {
          when(mockAtedConnector.retrieveAndCacheDisposeLiability(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(disposeLiabilityReturnJson))))
          val result = await(TestDisposeLiabilityReturnService.retrieveLiabilityReturn(formBundleNo1))
          result must be(Some(disposeLiabilityReturn))
        }

        "return None, if data is not-found in cache/ETMP, i.e. for any other status-code" in {
          when(mockAtedConnector.retrieveAndCacheDisposeLiability(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, responseJson = None)))
          val result = await(TestDisposeLiabilityReturnService.retrieveLiabilityReturn(formBundleNo2))
          result must be(None)
        }
      }

    }

    "cacheDisposeLiabilityReturnDate" must {

      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is saved in cache, i.e. for status-code OK" in {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnDate(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(disposeLiabilityReturnJson))))
          val result = await(TestDisposeLiabilityReturnService.cacheDisposeLiabilityReturnDate(formBundleNo1, updatedDate))
          result must be(Some(disposeLiabilityReturn))
        }

        "return None, if data is not saved in cache, i.e. for any other status-code" in {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnDate(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, responseJson = None)))
          val result = await(TestDisposeLiabilityReturnService.cacheDisposeLiabilityReturnDate(formBundleNo2, updatedDate))
          result must be(None)
        }
      }

    }

    "cacheDisposeLiabilityReturnHasBankDetails" must {

      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is saved in cache, i.e. for status-code OK" in {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnHasBank(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(disposeLiabilityReturnJson))))
          val result = await(TestDisposeLiabilityReturnService.cacheDisposeLiabilityReturnHasBankDetails(formBundleNo1, hasBankDetails = true))
          result must be(Some(disposeLiabilityReturn))
        }

        "return None, if data is not saved in cache, i.e. for any other status-code" in {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnHasBank(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, responseJson = None)))
          val result = await(TestDisposeLiabilityReturnService.cacheDisposeLiabilityReturnHasBankDetails(formBundleNo2, hasBankDetails = false))
          result must be(None)
        }
      }

    }

    "cacheDisposeLiabilityReturnBank" must {

      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is saved in cache, i.e. for status-code OK" in {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnBank(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(disposeLiabilityReturnJson))))
          val result = await(TestDisposeLiabilityReturnService.cacheDisposeLiabilityReturnBank(formBundleNo1, bankDetails1))
          result must be(Some(disposeLiabilityReturn))
        }

        "return None, if data is not saved in cache, i.e. for any other status-code" in {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnBank(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, responseJson = None)))
          val result = await(TestDisposeLiabilityReturnService.cacheDisposeLiabilityReturnBank(formBundleNo2, bankDetails1))
          result must be(None)
        }
      }

    }

    "calculateDraftDisposal" must {

      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is saved in cache, i.e. for status-code OK" in {
          when(mockAtedConnector.calculateDraftDisposal(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(disposeLiabilityReturnJson))))
          val result = await(TestDisposeLiabilityReturnService.calculateDraftDisposal(formBundleNo1))
          result must be(Some(disposeLiabilityReturn))
        }

        "return None, if data is not saved in cache, i.e. for any other status-code" in {
          when(mockAtedConnector.calculateDraftDisposal(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, responseJson = None)))
          val result = await(TestDisposeLiabilityReturnService.calculateDraftDisposal(formBundleNo2))
          result must be(None)
        }
      }

    }

    "submitDraftDisposeLiability" must {
      "for valid form-bundle-number" must {

        "return EditLiabilityReturnsResponseModel, if submit status is OK" in {
          val jsonEtmpResponse =
            s"""
               |{
               |  "processingDate": "2001-12-17T09:30:47Z",
               |  "liabilityReturnResponse": [
               |    {
               |      "mode": "Post",
               |      "oldFormBundleNumber": "$formBundleNo1",
               |      "MDTPKey": "1",
               |      "liabilityAmount": 1234.12,
               |      "amountDueOrRefund": 200.00,
               |      "paymentReference": "aaaaaaaaaaaaaa",
               |      "formBundleNumber": "012345678912"
               |    }
               |  ],
               |  "accountBalance": 10000.20
               |}
            """.stripMargin

          val response: JsValue = Json.parse(jsonEtmpResponse)

          when(mockDataCacheConnector.clearCache()(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
          when(mockDataCacheConnector.saveFormData[EditLiabilityReturnsResponseModel]
            (Matchers.eq(SubmitEditedLiabilityReturnsResponseFormId), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(response.as[EditLiabilityReturnsResponseModel]))
          when(mockAtedConnector.submitDraftDisposeLiabilityReturn(Matchers.eq(formBundleNo1))(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(response))))
          val result = await(TestDisposeLiabilityReturnService.submitDraftDisposeLiability(formBundleNo1))
          result.liabilityReturnResponse.length must be(1)
        }

        "return empty EditLiabilityReturnsResponseModel, if submit status is not OK" in {
          when(mockAtedConnector.submitDraftDisposeLiabilityReturn(Matchers.eq(formBundleNo1))(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
          val result = await(TestDisposeLiabilityReturnService.submitDraftDisposeLiability(formBundleNo1))
          result.liabilityReturnResponse.length must be(0)
          result.liabilityReturnResponse must be(Nil)
        }
      }

    }

  }

}
