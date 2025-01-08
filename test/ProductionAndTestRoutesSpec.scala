/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec

import scala.io.Source
import scala.util.Using

class ProductionAndTestRoutesSpec extends PlaySpec {

  "Production routes" should {
    "not use any test routes in production" in {

      val prodRouteFileContents = Using.resource(Source.fromFile("conf/app.routes")) { source => source.getLines().map(_.trim.toLowerCase()).toList }

      val testRoutesInProdRoutes = prodRouteFileContents.filter(_.contains("test-only"))

      withClue(s"The following test-only routes were found in production: ${testRoutesInProdRoutes.mkString(", ")}. Move any test routes to testOnlyDoNotUseInAppConf.routes") {
        testRoutesInProdRoutes mustBe empty
      }
    }

    "not have any overlapping routes with test routes" in {
      val prodRouteFileContents = Using.resource(Source.fromFile("conf/app.routes")) { source => source.getLines().map(_.trim.toLowerCase()).filter(_.nonEmpty).toSet }
      val testRouteFileContents = Using.resource(Source.fromFile("conf/testOnlyDoNotUseInAppConf.routes")) { source => source.getLines().map(_.trim.toLowerCase()).filter(_.nonEmpty).toSet }


      val overlap = prodRouteFileContents.intersect(testRouteFileContents)
      withClue(s"Overlapping routes between production and test: ${overlap.mkString(", ")}") {
        overlap mustBe empty
      }
    }
  }

  "Test routes" should {
    "only used routes prefixed with /test-only/" in {
      val testRouteFileContents = Using.resource(Source.fromFile("conf/testOnlyDoNotUseInAppConf.routes")) { source => source.getLines().map(_.trim.toLowerCase()).toList }

      val invalidTestRoutes = testRouteFileContents.filter(line =>
        line.nonEmpty &&
          !line.startsWith("#") &&
          !line.startsWith("->") &&
          !line.contains("/test-only/")
      )

      withClue(s"The following routes in testOnlyDoNotUseInAppConf.routes are not prefixed with '/test-only/': ${invalidTestRoutes.mkString(", ")}. Ensure all routes are prefixed appropriately.") {
        invalidTestRoutes mustBe empty
      }
    }
  }

  "Application conf file" should {
    "not have any mention of test routes" in {
      val applicationConfFileContents = Using.resource(Source.fromFile("conf/application.conf")) { source => source.getLines().map(_.trim.toLowerCase()).toList }
      val testOnlyRoutesInAppConf = applicationConfFileContents.filter(_.contains("testonlydonotuseinappconf.routes"))

      withClue(s"The following test-only routes were found in application conf file: ${testOnlyRoutesInAppConf.mkString(", ")}. There should be no test routes present here.") {
        testOnlyRoutesInAppConf mustBe empty
      }
    }
  }
}