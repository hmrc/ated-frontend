package forms

import forms.PropertyDetailsForms.{propertyDetailsTaxAvoidanceForm, validatePropertyDetailsTaxAvoidance}
import org.scalatest.Matchers
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

class PropertyDetailsTaxAvoidanceSpec  extends UnitSpec with Matchers with OneAppPerSuite {

  implicit lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages = messagesApi.preferred(FakeRequest())

  "PropertyDetailsTaxAvoidanceForm" must {
    "throw error" when {
      "form is empty" in {
        val form = propertyDetailsTaxAvoidanceForm.bind(Map.empty[String, String])
        form.fold(
          hasErrors => {
            hasErrors.errors.length shouldBe 1
            hasErrors.errors.head.message shouldBe Messages("ated.property-details-period.isTaxAvoidance.error-field-name")
          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "taxAvoidance is selected as 'Yes' and fields are empty" in {
        val input: Map[String, String] = Map("isTaxAvoidance" -> "true")
        val form = propertyDetailsTaxAvoidanceForm.bind(input)
        validatePropertyDetailsTaxAvoidance(form).fold(
          hasErrors => {
            hasErrors.errors.length shouldBe 2
            hasErrors.errors.head.message shouldBe Messages("ated.property-details-period.taxAvoidanceScheme.error.empty")
            hasErrors.errors(1).message shouldBe Messages("ated.property-details-period.taxAvoidancePromoterReference.error.empty")
          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "taxAvoidance is selected as 'Yes' and fields have invalid data" in {
        val input: Map[String, String] = Map("isTaxAvoidance" -> "true",
          "taxAvoidanceScheme" -> "123456789",
          "taxAvoidancePromoterReference" -> "123456789")
        val form = propertyDetailsTaxAvoidanceForm.bind(input)
        validatePropertyDetailsTaxAvoidance(form).fold(
          hasErrors => {
            hasErrors.errors.length shouldBe 2
            hasErrors.errors.head.message shouldBe Messages("ated.property-details-period.taxAvoidanceScheme.error.wrong-length")
            hasErrors.errors(1).message shouldBe Messages("ated.property-details-period.taxAvoidancePromoterReference.error.wrong-length")
          },
          _ => {
            fail("There is a problem")
          }
        )
      }

      "taxAvoidance is selected as 'Yes' and fields have some random string as input" in {
        val input: Map[String, String] = Map("isTaxAvoidance" -> "true",
          "taxAvoidanceScheme" -> "asdfght",
          "taxAvoidancePromoterReference" -> "asdfght")

        val form = propertyDetailsTaxAvoidanceForm.bind(input)
        validatePropertyDetailsTaxAvoidance(form).fold(
          hasErrors => {
            hasErrors.errors.length shouldBe 2
            hasErrors.errors.head.message shouldBe Messages("ated.property-details-period.taxAvoidanceScheme.error.numbers")
            hasErrors.errors(1).message shouldBe Messages("ated.property-details-period.taxAvoidancePromoterReference.error.numbers")
          },
          _ => {
            fail("There is a problem")
          }
        )
      }
    }
  }
}
