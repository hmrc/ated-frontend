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

import builders.{ChangeLiabilityReturnBuilder, PropertyDetailsBuilder}
import config.ApplicationConfig
import connectors.{AtedConnector, DataCacheConnector}
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AtedConstants._

import scala.concurrent.Future

class ChangeLiabilityReturnServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val authContext: StandardAuthRetrievals = mock[StandardAuthRetrievals]
  implicit  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  val mockMcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val mockAtedConnector: AtedConnector = mock[AtedConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

  val changeLiabilityReturn: PropertyDetails = PropertyDetailsBuilder.getFullPropertyDetails("123456789012")
  val changeLiabilityReturnJson: JsValue = Json.toJson(changeLiabilityReturn)

  val address: PropertyDetailsAddress = ChangeLiabilityReturnBuilder.generatePropertyDetailsAddress
  val title1 = PropertyDetailsTitle("updatedTitle")
  val periodKey = 2015
  val period1: PropertyDetailsPeriod =  PropertyDetailsBuilder.getPropertyDetailsPeriodFull(periodKey).get
  val bankDetails1 = BankDetails()

  val formBundleNo1 = "123456789012"
  val formBundleNo2 = "123456789000"

  class Setup {
    val testChangeLiabilityReturnService: ChangeLiabilityReturnService = new ChangeLiabilityReturnService (
    mockMcc,
    mockAtedConnector,
    mockDataCacheConnector
    )
  }

  override def beforeEach(): Unit = {
  }

  "ChangeLiabilityReturnService" must {
    "retrieveLiabilityReturn" must {
      "for valid form-bundle-number" must {

        "return Some(ChangeLiabilityReturn), if data is found in cache/ETMP, i.e. for status-code OK" in new Setup {
          when(mockAtedConnector.retrieveAndCacheLiabilityReturn(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, changeLiabilityReturnJson.toString)))
          val result: Option[PropertyDetails] = await(testChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(formBundleNo1))
          result must be(Some(changeLiabilityReturn))
        }

        "return None, if data is not-found in cache/ETMP, i.e. for any other status-code" in new Setup {
          when(mockAtedConnector.retrieveAndCacheLiabilityReturn(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))
          val result: Option[PropertyDetails] = await(testChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(formBundleNo2))
          result must be(None)
        }
      }

      "for valid form-bundle-number and previous return is selected" must {
        "return Some(ChangeLiabilityReturn), if data is found in cache/ETMP, i.e. for status-code OK" in new Setup {
          when(mockAtedConnector.retrieveAndCachePreviousLiabilityReturn(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, changeLiabilityReturnJson.toString)))
          val result: Option[PropertyDetails] = await(testChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(
            formBundleNo1, Some(true), Some(SelectPeriod(Some("2015")))))
          result must be(Some(changeLiabilityReturn))
        }

        "return None, if data is not-found in cache/ETMP, i.e. for any other status-code" in new Setup {
          when(mockAtedConnector.retrieveAndCachePreviousLiabilityReturn(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))
          val result: Option[PropertyDetails] = await(testChangeLiabilityReturnService.retrieveSubmittedLiabilityReturnAndCache(
            formBundleNo2, Some(true), Some(SelectPeriod(Some("2015")))))
          result must be(None)
        }
      }
    }


    "cacheChangeLiabilityReturnHasBankDetails" must {
      "for valid form-bundle-number" must {
        "return Some(ChangeLiabilityReturn), if data is saved in cache, i.e. for status-code OK" in new Setup {
          when(mockAtedConnector.cacheDraftChangeLiabilityReturnHasBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, changeLiabilityReturnJson.toString)))
          val result: Option[PropertyDetails] = await(testChangeLiabilityReturnService.cacheChangeLiabilityReturnHasBankDetails(formBundleNo1, updatedValue = true))
          result must be(Some(changeLiabilityReturn))
        }

        "return None, if data is not saved in cache, i.e. for any other status-code" in new Setup {
          when(mockAtedConnector.cacheDraftChangeLiabilityReturnHasBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))
          val result: Option[PropertyDetails] = await(testChangeLiabilityReturnService.cacheChangeLiabilityReturnHasBankDetails(formBundleNo2, updatedValue = false))
          result must be(None)
        }
      }

    }

    "cacheChangeLiabilityReturnBank" must {
      "for valid form-bundle-number" must {
        "return Some(ChangeLiabilityReturn), if data is saved in cache, i.e. for status-code OK" in new Setup {
          when(mockAtedConnector.cacheDraftChangeLiabilityReturnBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, changeLiabilityReturnJson.toString)))
          val result: Option[PropertyDetails] = await(testChangeLiabilityReturnService.cacheChangeLiabilityReturnBank(formBundleNo1, bankDetails1))
          result must be(Some(changeLiabilityReturn))
        }

        "return None, if data is not saved in cache, i.e. for any other status-code" in new Setup {
          when(mockAtedConnector.cacheDraftChangeLiabilityReturnBank(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))
          val result: Option[PropertyDetails] = await(testChangeLiabilityReturnService.cacheChangeLiabilityReturnBank(formBundleNo2, bankDetails1))
          result must be(None)
        }
      }

    }

    "submitDraftChangeLiability" must {
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

          when(mockDataCacheConnector.clearCache()(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "")))
          when(mockDataCacheConnector.saveFormData[EditLiabilityReturnsResponseModel]
            (ArgumentMatchers.eq(SubmitEditedLiabilityReturnsResponseFormId), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(response.as[EditLiabilityReturnsResponseModel]))
          when(mockAtedConnector.submitDraftChangeLiabilityReturn(ArgumentMatchers.eq(formBundleNo1))(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, response.toString)))
          val result: EditLiabilityReturnsResponseModel = await(testChangeLiabilityReturnService.submitDraftChangeLiability(formBundleNo1))
          result.liabilityReturnResponse.length must be(1)
        }

        "return empty EditLiabilityReturnsResponseModel, if submit status is not OK" in new Setup {
          when(mockAtedConnector.submitDraftChangeLiabilityReturn(ArgumentMatchers.eq(formBundleNo1))(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))
          val result: EditLiabilityReturnsResponseModel = await(testChangeLiabilityReturnService.submitDraftChangeLiability(formBundleNo1))
          result.liabilityReturnResponse.length must be(0)
          result.liabilityReturnResponse must be(Nil)
        }
      }

    }

  }

}
