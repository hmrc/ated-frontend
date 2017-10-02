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

package builders

import java.util.concurrent.ConcurrentLinkedQueue

import config.AtedFrontendAuditConnector
import uk.gov.hmrc.play.audit.model.Audit._
import uk.gov.hmrc.play.audit.model.{Audit, AuditAsMagnet, DataEvent}
import uk.gov.hmrc.http.HeaderCarrier

class TestAudit extends Audit("test", AtedFrontendAuditConnector) {

  var capturedTxName: String = ""
  var capturedInputs: Map[String, String] = Map.empty
  private val dataEvents = new ConcurrentLinkedQueue[DataEvent]

  override def as[A](auditMagnet: AuditAsMagnet[A])(body: Body[A])(implicit hc: HeaderCarrier): A = {
    this.capturedTxName = auditMagnet.txName
    this.capturedInputs = auditMagnet.inputs
    super.as(auditMagnet)(body)
  }

  def capturedDataEvents: Seq[DataEvent] = dataEvents.toArray(new Array[DataEvent](0)).toSeq

  def captureDataEvent(event: DataEvent) = {
    this.dataEvents.add(event)
    ()
  }

  override def sendDataEvent: (DataEvent) => Unit = captureDataEvent
}
