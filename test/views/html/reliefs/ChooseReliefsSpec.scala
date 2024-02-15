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

package views.html.reliefs

import config.ApplicationConfig
import forms.ReliefForms
import forms.ReliefForms._
import models.{Reliefs, StandardAuthRetrievals}
import java.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.Json
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}

class ChooseReliefsSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {
  implicit lazy val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val periodKey = 2017
  val periodKey2020 = 2020
  val periodStartDate = LocalDate.now()

  "choose relief view" must {

    "have correct page title" in {
      doc.title mustBe messages("ated.choose-reliefs.title") + " - Submit and view your ATED returns - GOV.UK"
    }
    "have correct heading" in {
      doc.getElementsByTag("h1").text must include(messages("ated.choose-reliefs.header"))
    }
    "have correct caption" in {
      doc.getElementsByClass( "govuk-caption-xl").text must include(messages("ated.choose-reliefs.subheader"))
    }
    "have a backLink" in {
      val backLink = new CssSelector("a.govuk-back-link")
      doc must backLink
    }
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
      doc.getElementsByAttributeValue("for", "socialHousing").text() mustBe "Social housing"
    }

    "display social housing start date" in {
      doc must haveElementWithId("socialHousingDate")
    }

    "display social housing for 2020" in {
      doc2020.getElementsByAttributeValue("for", "socialHousing").text() mustBe "Provider of social housing or housing co-operative"
    }

    "display social housing start date for 2020" in {
      doc2020 must haveElementWithId("conditional-socialHousing")
    }

    "display equity release scheme" in {
      doc must haveElementWithId("equityRelease")
    }

    "display equity release scheme start date" in {
      doc must haveElementWithId("equityReleaseDate")
    }


    "display error" when {
      "rental business is selected but no date is populated" in {
        haveChooseReliefFormError("rentalBusiness")
      }

      "rental business start date is invalid" in {
        haveChooseReliefStartDateFormError("rentalBusiness")
      }

      "open to public is selected but no date is populated" in {
        haveChooseReliefFormError("openToPublic")
      }

      "open to public start date is invalid" in {
        haveChooseReliefStartDateFormError("openToPublic")
      }

      "property developer is selected but no date is populated" in {
        haveChooseReliefFormError("propertyDeveloper")
      }

      "property developer start date is invalid" in {
        haveChooseReliefStartDateFormError("propertyDeveloper")
      }

      "property trading is selected but no date is populated" in {
        haveChooseReliefFormError("propertyTrading")
      }

      "property trading start date is invalid" in {
        haveChooseReliefStartDateFormError("propertyTrading")
      }

      "lending is selected but no date is populated" in {
        haveChooseReliefFormError("lending")
      }

      "lending start date is invalid" in {
        haveChooseReliefStartDateFormError("lending")
      }

      "employee occupation is selected but no date is populated" in {
        haveChooseReliefFormError("employeeOccupation")
      }

      "employee occupation start date is invalid" in {
        haveChooseReliefStartDateFormError("employeeOccupation")
      }

      "farm houses is selected but no date is populated" in {
        haveChooseReliefFormError("farmHouses")
      }

      "farm houses start date is invalid" in {
        haveChooseReliefStartDateFormError("farmHouses")
      }

      "social housing is selected but no date is populated" in {
        haveChooseReliefFormError("socialHousing")
      }

      "social housing start date is invalid" in {
        haveChooseReliefStartDateFormError("socialHousing")
      }

      "equity release is selected but no date is populated" in {
        haveChooseReliefFormError("equityRelease")
      }

      "equity release start date is invalid" in {
        haveChooseReliefStartDateFormError("equityRelease")
      }
    }
  }

  val injectedViewInstance: chooseReliefs = app.injector.instanceOf[views.html.reliefs.chooseReliefs]

  override def haveErrorSummary(expectedText: String) = new CssSelectorWithTextMatcher(expectedText, "#main-content > div > div > div > div > div > ul > li > a")
  override def haveErrorNotification(expectedText: String) = new CssSelectorWithTextMatcher(expectedText, ".govuk-error-message")


  def haveChooseReliefFormError(field: String): Unit = {
    val fieldStartDate = field + "Date"
    val formWithErrors: Form[Reliefs] = validateForm(reliefsForm.bind(Json.obj("periodKey" -> periodKey, field -> true), Int.MaxValue))

    def view: Html = injectedViewInstance(periodKey, formWithErrors, periodStartDate, Html(""), Some("backLink"))

    val errorDoc = doc(view)

    errorDoc must haveErrorSummary(messages(s"ated.choose-reliefs.error.date.mandatory.$fieldStartDate"))
    errorDoc must haveErrorNotification("Error: " + messages(s"ated.choose-reliefs.error.date.mandatory.$fieldStartDate"))
  }

  def haveChooseReliefStartDateFormError(field: String): Unit = {
    val fieldStartDate = field + "Date"
    val formWithErrors: Form[Reliefs] = validateForm(reliefsForm.bind(Json.obj("periodKey" -> periodKey, field -> true,
      fieldStartDate -> Map("day" -> "1")), Int.MaxValue))

    def view: Html = injectedViewInstance(periodKey, formWithErrors, periodStartDate, Html(""), Some("backLink"))

    val errorDoc = doc(view)

    errorDoc must haveErrorSummary(messages(s"ated.choose-reliefs.error.date.mandatory.$fieldStartDate"))
    errorDoc must haveErrorNotification("Error: " + messages(s"ated.choose-reliefs.error.date.mandatory.$fieldStartDate"))
  }

  val reliefsForm: Form[Reliefs] = ReliefForms.reliefsForm

  override def view: Html = injectedViewInstance(periodKey, reliefsForm, periodStartDate, Html(""), Some("backLink"))
  def view2020: Html = injectedViewInstance(periodKey2020, reliefsForm, periodStartDate, Html(""), Some("backLink"))

  def doc2020: Document = Jsoup.parse(view2020.toString())

}
