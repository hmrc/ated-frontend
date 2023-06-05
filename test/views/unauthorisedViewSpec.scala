package views

import config.ApplicationConfig
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.i18n.{Messages, MessagesApi}
import play.twirl.api.Html
import views.html.unauthorised

class unauthorisedViewSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {

  implicit val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  class Test(sa: Boolean){
    implicit val isSa = sa
    val injectedViewInstance: unauthorised = app.injector.instanceOf[views.html.unauthorised]
  }

  "unauthorised" when {
    "isSa is true" must {
      "have the correct title" in new Test(true) {
        val html = injectedViewInstance()
        val document = Jsoup.parse(html.toString())
        document.title mustBe "You need to sign in with a different Gateway ID"
      }
    }
  }

}
