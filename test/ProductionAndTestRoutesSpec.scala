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

class ProductionAndTestRoutesSpec extends PlaySpec {

  "Production routes" should {
    "not use any test routes in production" in {
      val routesFileContents = Source.fromFile("conf/app.routes").getLines().toList
      val testOnlyRoutes = routesFileContents.filter(_.contains("test-only"))

      withClue(s"The following test-only routes were found in production: ${testOnlyRoutes.mkString(", ")}. Move any test routes to testOnlyDoNotUseInAppConf.routes") {
        testOnlyRoutes mustBe empty
      }
    }

    "Test routes" should {
      "only used routes prefixed with /test-only/" in {
        val testRoutesFileContents = Source.fromFile("conf/testOnlyDoNotUseInAppConf.routes").getLines().toList

        val invalidTestRoutes = testRoutesFileContents.filter(line =>
          line.trim.nonEmpty &&
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
        val applicationConfFileContents = Source.fromFile("conf/application.conf").getLines().toList
        val testOnlyRoutes = applicationConfFileContents.filter(_.contains("testOnlyDoNotUseInAppConf.routes"))
        withClue(s"The following test-only routes were found in application conf file: ${testOnlyRoutes.mkString(", ")}. There should be no test routes present here.") {
          testOnlyRoutes mustBe empty
        }
      }
    }
  }
}
