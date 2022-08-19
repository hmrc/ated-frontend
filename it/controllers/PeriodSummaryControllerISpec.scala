
package controllers

import config.ApplicationConfig
import config.featureswitch.FeatureSwitch
import helpers.IntegrationBase
import helpers.stubs.{AuthAudit, KeyStore}
import play.api.http.{HeaderNames => HN}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HeaderNames

class PeriodSummaryControllerISpec extends IntegrationBase with AuthAudit with KeyStore {
  val sessionCookie = getCookie()

  "respond with a status of 200" when {
    "viewing the period summary of 2019" in {
      val period2019 = 2019

      stubAuthAudit()
      stubKeyStore()
      stubGet("/ated/XN1200000100001/returns/partial-summary", 200,
        s"""{
          |"allReturns" : [
          | {
          |   "periodKey" : $period2019,
          |   "draftReturns" : [
          |     {
          |       "periodKey" : $period2019,
          |       "id" : "testId",
          |       "description" : "Social housing",
          |       "returnType" : "Relief"
          |     }
          |   ]
          | }
          |]
          |}""".stripMargin)

      val controllerUrl = controllers.routes.PeriodSummaryController.view(period2019).url

      val resp: WSResponse = await(client(controllerUrl)
        .withCookies(encryptedCookie(sessionCookie))
        .addHttpHeaders(HN.SET_COOKIE -> getSessionCookie())
        //.addHttpHeaders(HeaderNames.xSessionId -> sessionId)
        .get
      )

      resp.status mustBe 200
      resp.body.contains("Social housing") mustBe true
    }

    "viewing the period summary of 2020 with the social housing feature flag disabled" in {
      app.injector.instanceOf[ApplicationConfig].disable(FeatureSwitch.CooperativeHousing)

      val period2020 = 2020

      stubAuthAudit()
      stubKeyStore()
      stubGet("/ated/XN1200000100001/returns/partial-summary", 200,
        s"""{
           |"allReturns" : [
           | {
           |   "periodKey" : $period2020,
           |   "draftReturns" : [
           |     {
           |       "periodKey" : $period2020,
           |       "id" : "testId",
           |       "description" : "Social housing",
           |       "returnType" : "Relief"
           |     }
           |   ]
           | }
           |]
           |}""".stripMargin)

      val controllerUrl = controllers.routes.PeriodSummaryController.view(period2020).url

      val resp: WSResponse = await(client(controllerUrl)
        .withCookies(encryptedCookie(sessionCookie))
        .addHttpHeaders(HN.SET_COOKIE -> getSessionCookie())
        //.addHttpHeaders(HeaderNames.xSessionId -> sessionId)
        .get
      )

      resp.status mustBe 200
      resp.body.contains("Social housing") mustBe true
    }

    "viewing the period summary of 2020 with the social housing feature flag enabled" in {
      app.injector.instanceOf[ApplicationConfig].enable(FeatureSwitch.CooperativeHousing)

      val period2020 = 2020

      stubAuthAudit()
      stubKeyStore()
      stubGet("/ated/XN1200000100001/returns/partial-summary", 200,
        s"""{
          |"allReturns" : [
          | {
          |   "periodKey" : $period2020,
          |   "draftReturns" : [
          |     {
          |       "periodKey" : $period2020,
          |       "id" : "testId",
          |       "description" : "Social housing",
          |       "returnType" : "Relief"
          |     }
          |   ]
          | }
          |]
          |}""".stripMargin)

      val controllerUrl = controllers.routes.PeriodSummaryController.view(period2020).url

      val resp: WSResponse = await(client(controllerUrl)
        .withCookies(encryptedCookie(sessionCookie))
        .addHttpHeaders(HN.SET_COOKIE -> getSessionCookie())
        //.addHttpHeaders(HeaderNames.xSessionId -> sessionId)
        .get
      )

      resp.status mustBe 200
      resp.body.contains("Social housing") mustBe false
      resp.body.contains("Provider of social housing or housing co-operative") mustBe true
    }
  }
}