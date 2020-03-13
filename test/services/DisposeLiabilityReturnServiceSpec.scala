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

import builders.DisposeLiabilityReturnBuilder
import connectors.{AtedConnector, DataCacheConnector}
import testhelpers.MockAuthUtil
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants._

import scala.concurrent.Future

class DisposeLiabilityReturnServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockAuthUtil {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]

  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val formBundleNo1: String = "123456789012"
  val formBundleNo2: String = "123456789000"
  val periodKey = 2015
  val disposeLiabilityReturn: DisposeLiabilityReturn = DisposeLiabilityReturnBuilder.generateDisposeLiabilityReturn("123456789012")
  val disposeLiabilityReturnJson: JsValue = Json.toJson(disposeLiabilityReturn)
  val updatedDate: DisposeLiability = DisposeLiabilityReturnBuilder.generateDisposalDate(periodKey)
  val bankDetails1: BankDetails = BankDetails()

  class Setup {
  val testDisposeLiabilityReturnService: DisposeLiabilityReturnService = new DisposeLiabilityReturnService(
  mockAtedConnector,
  mockDataCacheConnector
  )
}

  override def beforeEach: Unit = {
    reset(mockAtedConnector)
    reset(mockDataCacheConnector)
  }


  "DisposeLiabilityReturnService" must {
    "retrieveLiabilityReturn" must {

      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is found in cache/ETMP, i.e. for status-code OK" in new Setup {
          when(mockAtedConnector.retrieveAndCacheDisposeLiability(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(disposeLiabilityReturnJson))))
          val result: Option[DisposeLiabilityReturn] = await(testDisposeLiabilityReturnService.retrieveLiabilityReturn(formBundleNo1))
          result must be(Some(disposeLiabilityReturn))
        }

        "return None, if data is not-found in cache/ETMP, i.e. for any other status-code" in new Setup {
          when(mockAtedConnector.retrieveAndCacheDisposeLiability(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, responseJson = None)))
          val result: Option[DisposeLiabilityReturn] = await(testDisposeLiabilityReturnService.retrieveLiabilityReturn(formBundleNo2))
          result must be(None)
        }
      }

    }

    "cacheDisposeLiabilityReturnDate" must {

      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is saved in cache, i.e. for status-code OK" in new Setup {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(disposeLiabilityReturnJson))))
          val result: Option[DisposeLiabilityReturn] = await(testDisposeLiabilityReturnService.cacheDisposeLiabilityReturnDate(formBundleNo1, updatedDate))
          result must be(Some(disposeLiabilityReturn))
        }

        "return None, if data is not saved in cache, i.e. for any other status-code" in new Setup {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnDate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, responseJson = None)))
          val result: Option[DisposeLiabilityReturn] = await(testDisposeLiabilityReturnService.cacheDisposeLiabilityReturnDate(formBundleNo2, updatedDate))
          result must be(None)
        }
      }

    }

    "cacheDisposeLiabilityReturnHasBankDetails" must {

      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is saved in cache, i.e. for status-code OK" in new Setup {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnHasBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(disposeLiabilityReturnJson))))
          val result: Option[DisposeLiabilityReturn] = await(testDisposeLiabilityReturnService
            .cacheDisposeLiabilityReturnHasBankDetails(formBundleNo1, hasBankDetails = true))
          result must be(Some(disposeLiabilityReturn))
        }

        "return None, if data is not saved in cache, i.e. for any other status-code" in new Setup {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnHasBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, responseJson = None)))
          val result: Option[DisposeLiabilityReturn] = await(testDisposeLiabilityReturnService
            .cacheDisposeLiabilityReturnHasBankDetails(formBundleNo2, hasBankDetails = false))
          result must be(None)
        }
      }

    }

    "cacheDisposeLiabilityReturnBank" must {

      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is saved in cache, i.e. for status-code OK" in new Setup {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(disposeLiabilityReturnJson))))
          val result: Option[DisposeLiabilityReturn] = await(testDisposeLiabilityReturnService.cacheDisposeLiabilityReturnBank(formBundleNo1, bankDetails1))
          result must be(Some(disposeLiabilityReturn))
        }

        "return None, if data is not saved in cache, i.e. for any other status-code" in new Setup {
          when(mockAtedConnector.cacheDraftDisposeLiabilityReturnBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, responseJson = None)))
          val result: Option[DisposeLiabilityReturn] = await(testDisposeLiabilityReturnService.cacheDisposeLiabilityReturnBank(formBundleNo2, bankDetails1))
          result must be(None)
        }
      }

    }

    "calculateDraftDisposal" must {

      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is saved in cache, i.e. for status-code OK" in new Setup {
          when(mockAtedConnector.calculateDraftDisposal(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(disposeLiabilityReturnJson))))
          val result: Option[DisposeLiabilityReturn] = await(testDisposeLiabilityReturnService.calculateDraftDisposal(formBundleNo1))
          result must be(Some(disposeLiabilityReturn))
        }

        "return None, if data is not saved in cache, i.e. for any other status-code" in new Setup {
          when(mockAtedConnector.calculateDraftDisposal(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, responseJson = None)))
          val result: Option[DisposeLiabilityReturn] = await(testDisposeLiabilityReturnService.calculateDraftDisposal(formBundleNo2))
          result must be(None)
        }
      }

    }

    "submitDraftDisposeLiability" must {
      "for valid form-bundle-number" must {

        "return EditLiabilityReturnsResponseModel, if submit status is OK" in new Setup {
          val jsonEtmpResponse: String =
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

          when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
          when(mockDataCacheConnector.saveFormData[EditLiabilityReturnsResponseModel]
            (ArgumentMatchers.eq(SubmitEditedLiabilityReturnsResponseFormId), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(response.as[EditLiabilityReturnsResponseModel]))
          when(mockAtedConnector.submitDraftDisposeLiabilityReturn(ArgumentMatchers.eq(formBundleNo1))(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(response))))
          val result: EditLiabilityReturnsResponseModel = await(testDisposeLiabilityReturnService.submitDraftDisposeLiability(formBundleNo1))
          result.liabilityReturnResponse.length must be(1)
        }

        "return empty EditLiabilityReturnsResponseModel, if submit status is not OK" in new Setup {
          when(mockAtedConnector.submitDraftDisposeLiabilityReturn(ArgumentMatchers.eq(formBundleNo1))(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
          val result: EditLiabilityReturnsResponseModel = await(testDisposeLiabilityReturnService.submitDraftDisposeLiability(formBundleNo1))
          result.liabilityReturnResponse.length must be(0)
          result.liabilityReturnResponse must be(Nil)
        }
      }

    }

  }

}
