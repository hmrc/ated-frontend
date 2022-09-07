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

package views.html.propertyDetails

import config.ApplicationConfig
import forms.PropertyDetailsForms
import models.StandardAuthRetrievals
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Messages, MessagesApi}
import play.twirl.api.Html
import testhelpers.{AtedViewSpec, MockAuthUtil}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}

class PropertyDetailsAcquisitionSpec extends AtedViewSpec with MockitoSugar with MockAuthUtil {

  //factor out AtedViewSpec

  override implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  implicit val authContext: StandardAuthRetrievals = organisationStandardRetrievals

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  private val formWithErrors = PropertyDetailsForms.propertyDetailsAcquisitionForm.withError("anAcquisition", messages("ated.property-details-value.anAcquisition.error-field-name"))

  private val form = PropertyDetailsForms.propertyDetailsAcquisitionForm

  override def view: Html = injectedViewInstance("", 0, form, None, Html(""), Some("backLink"))
  def viewWithErrors: Html = injectedViewInstance("", 0, formWithErrors, None, Html(""), Some("backLink"))

  override def doc: Document = Jsoup.parse(view.toString())
  override def doc(view: Html): Document = Jsoup.parse(view.toString())

  def docWithErrors: Document = Jsoup.parse(viewWithErrors.toString())

  val authMock: Enrolments ~ Some[AffinityGroup] ~ Some[String] = authResultDefault(AffinityGroup.Organisation, defaultEnrolmentSet)
  setAuthMocks(authMock)
  val injectedViewInstance = app.injector.instanceOf[views.html.propertyDetails.propertyDetailsAcquisition]

  override def haveBackLink = new CssSelector("a[id=backLinkHref]")

  "The property details acquisition view for a valid form" must {
    "have the correct page title" in {
      doc.title mustBe (messages("ated.property-details-value.anAcquisition.title") + " - GOV.UK")
    }

    "have the correct header" in {
      doc.title mustBe (messages("ated.property-details-value.anAcquisition.header") + " - GOV.UK")
    }

    "have the correct pre heading" in {
      doc.title(messages("ated.property-details.pre-header"))
    }

    "have a backlink" in {
      doc.getElementsByClass("govuk-back-link").text mustBe "Back"
    }

    "have a continue button" in {
      doc.getElementsByClass("govuk-button").text mustBe "Save and continue"
    }

    "have a yes/no radio button" in {
      doc.getElementsByAttributeValue("for","anAcquisition").text() mustBe messages("ated.property-details-value.yes")
      doc.getElementsByAttributeValue("for","anAcquisition-2").text() mustBe messages("ated.property-details-value.no")
    }

    "have the correct paragraphs present" in {
      doc.getElementsByTag("p").hasText === messages("ated.property-details-value.anAcquisition.what")
      doc.getElementsByTag("p").hasText === messages("ated.property-details-value.anAcquisition.what.text1")
      doc.getElementsByTag("p").hasText === messages("ated.property-details-value.anAcquisition.what.text2")
      doc.getElementsByTag("p").hasText === messages("ated.property-details-value.anAcquisition.what.text3")
    }

    "have the correct errors" in {
      docWithErrors.title mustBe ("Error: " + messages("ated.property-details-value.anAcquisition.title") + " - GOV.UK")
      docWithErrors.getElementsByClass("govuk-error-message").text() mustBe ("Error: " + messages("ated.property-details-value.anAcquisition.error-field-name"))
      docWithErrors.getElementsByClass("govuk-error-summary__list").text() mustBe messages("ated.property-details-value.anAcquisition.error-field-name")
    }
  }

}
