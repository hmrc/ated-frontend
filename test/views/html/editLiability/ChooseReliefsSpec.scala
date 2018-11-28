/*
 * Copyright 2018 HM Revenue & Customs
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

package views.html.editLiability

import forms.ReliefForms
import models.Reliefs
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.libs.json.Json
import play.twirl.api.Html
import utils.viewHelpers.AtedViewSpec

class ChooseReliefsSpec extends AtedViewSpec {

  val periodKey = 2017
  val periodStartDate = new LocalDate()

  "choose relief view" must {
    behave like pageWithTitle(messages("ated.choose-reliefs.title"))
    behave like pageWithHeader(messages("ated.choose-reliefs.header"))
    behave like pageWithPreHeading(messages("ated.choose-reliefs.subheader"))
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm(s"/ated/reliefs/$periodKey/send")
  }

  "choose relief page" must {
    "display rental business checkbox" in {
      doc must haveElementWithId("rentalBusiness")
    }

    "display rental business start date" in {
      doc must haveElementWithId("rentalBusinessDate")
    }

    "display open to the public" in {
      doc must haveElementWithId("openToPublic")
    }

    "display open to the public start date" in {
      doc must haveElementWithId("openToPublicDate")
    }

    "display property developers" in {
      doc must haveElementWithId("propertyDeveloper")
    }

    "display property developer start date" in {
      doc must haveElementWithId("propertyDeveloperDate")
    }

    "display property trading" in {
      doc must haveElementWithId("propertyTrading")
    }

    "display property trading start date" in {
      doc must haveElementWithId("propertyTradingDate")
    }

    "display Lending" in {
      doc must haveElementWithId("lending")
    }

    "display Lending start date" in {
      doc must haveElementWithId("lendingDate")
    }

    "display employee occupation" in {
      doc must haveElementWithId("employeeOccupation")
    }

    "display employee occupation start date" in {
      doc must haveElementWithId("employeeOccupationDate")
    }

    "display farm houses" in {
      doc must haveElementWithId("farmHouses")
    }

    "display farm houses start date" in {
      doc must haveElementWithId("farmHousesDate")
    }

    "display social housing" in {
      doc must haveElementWithId("socialHousing")
    }

    "display social housing start date" in {
      doc must haveElementWithId("socialHousingDate")
    }

    "display equity release scheme" in {
      doc must haveElementWithId("equityRelease")
    }

    "display equity release scheme start date" in {
      doc must haveElementWithId("equityReleaseDate")
    }

    "display error" when {
      "rental business is selected but not date is populated" in {
        val formWithErrors: Form[Reliefs] = ReliefForms.reliefsForm.bind(Json.obj("periodKey" -> periodKey, "rentalBusiness" -> true))
        def view: Html = views.html.reliefs.chooseReliefs(periodKey, formWithErrors, periodStartDate, Some("backLink"))
        println("\n\n\n")
        println(doc(view))
      }
    }
  }




  val reliefsForm: Form[Reliefs] = ReliefForms.reliefsForm
  override def view: Html = views.html.reliefs.chooseReliefs(periodKey, reliefsForm, periodStartDate, Some("backLink"))

}
