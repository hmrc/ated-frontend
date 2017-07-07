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

package utils

import play.api.Play
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.RunMode

case class FeatureSwitch(name: String, enabled: Boolean)

object FeatureSwitch extends RunMode {
  import play.api.Play.current

  def forName(name: String) = {
    FeatureSwitch(name, isEnabled(name))
  }

  def isEnabled(name: String) = {
    val sysPropValue = sys.props.get(systemPropertyName(name))
    sysPropValue match {
      case Some(x) => x.toBoolean
      case None => Play.configuration.getBoolean(confPropertyName(name)).getOrElse(false)
    }
  }

  def enable(switch: FeatureSwitch): FeatureSwitch = {
    setProp(switch.name, true)
  }

  def disable(switch: FeatureSwitch): FeatureSwitch = setProp(switch.name, false)

  def setProp(name: String, value: Boolean): FeatureSwitch = {
    val systemProps = sys.props.+= ((systemPropertyName(name), value.toString))
    forName(name)
  }

  def confPropertyName(name: String) = s"features.$name"
  def systemPropertyName(name: String) = s"features.$name"

  implicit val format = Json.format[FeatureSwitch]
}

object AtedFeatureSwitches {
  def api11 = FeatureSwitch.forName("api_11")
  def chargeableReturns = FeatureSwitch.forName("chargeable_returns")


  def byName(name: String): Option[FeatureSwitch] = name match {
    case "api_11" => Some(api11)
    case "chargeable_returns" => Some(chargeableReturns)
    case _ => None
  }

}
