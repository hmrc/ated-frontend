/*
 * Copyright 2022 HM Revenue & Customs
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

import models.requests.{ListLinks, NavContent, NavLinks}

object PartialFactory {

  def notificationBadgeCount(messageCount: Int): String = {
    messageCount match {
      case 0 => "0"
      case count if count > 99 => "+99"
      case _ => s"${messageCount}"
    }
  }

  def formsNav(form: NavLinks): ListLinks = {
    form.alerts match {
      case Some(alert) if alert.equals(0) => ListLinks("", "", showBoolean = Some(false))
      case Some(alert) => ListLinks(form.en, form.url, Some(notificationBadgeCount(alert)))
      case _ => ListLinks("", "", showBoolean = Some(false))
    }
  }

  def partialList(navLinks: NavContent): Seq[ListLinks] = {
        Seq(
            ListLinks(navLinks.home.en, navLinks.home.url),
            ListLinks(navLinks.account.en, navLinks.account.url),
            ListLinks(navLinks.messages.en, navLinks.messages.url, Some(notificationBadgeCount(navLinks.messages.alerts.getOrElse(0)))),
            ListLinks(navLinks.help.en, navLinks.help.url)
          )
  }

}
