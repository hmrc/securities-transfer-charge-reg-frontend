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

package controllers.actions

import base.{Fixtures, SpecBase}
import org.scalatest.concurrent.IntegrationPatience
import play.api.mvc.BodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.auth.core.retrieve.ItmpName
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolments}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.IndividualCheckImpl
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcAuthRequest

import scala.concurrent.{ExecutionContext, Future}

class IndividualCheckSpec extends SpecBase with IntegrationPatience {

  implicit val ec: ExecutionContext = Fixtures.ec

  class Harness(parser: BodyParsers.Default, redirects: Redirects, authConnector: Fixtures.FakeAuthConnectorSuccess)
    extends IndividualCheckImpl(parser, redirects, authConnector)(ec)
  {
    def callFilter[A](req: StcAuthRequest[A]): Future[Option[play.api.mvc.Result]] = filter(req)
  }

  "IndividualCheck filter" - {

    "must allow when confidence >= L250, name present and nino present" in {
      val app = applicationBuilder().build()
      running(app) {
        val parser = app.injector.instanceOf[BodyParsers.Default]
        val redirects = app.injector.instanceOf[Redirects]

        val itmpName = ItmpName(Some("First"), None, Some("Last"))
        val stcReq = Fixtures.fakeStcAuthRequest(request = FakeRequest(), confidenceLevelOverride = ConfidenceLevel.L250, maybeNinoOverride = Some("AA123456A"), maybeNameOverride = Some(itmpName))

        val harness = new Harness(parser, redirects, new Fixtures.FakeAuthConnectorSuccess(()))

        val result = harness.callFilter(stcReq).futureValue

        result must not be defined
      }
    }

    "must redirect when name is missing or incomplete" in {
      val app = applicationBuilder().build()
      running(app) {
        val parser = app.injector.instanceOf[BodyParsers.Default]
        val redirects = app.injector.instanceOf[Redirects]

        val itmpName = ItmpName(None, None, Some("Last"))
        val stcReq = Fixtures.fakeStcAuthRequest(request = FakeRequest(), confidenceLevelOverride = ConfidenceLevel.L250, maybeNinoOverride = Some("AA123456A"), maybeNameOverride = Some(itmpName))

        val harness = new Harness(parser, redirects, new Fixtures.FakeAuthConnectorSuccess(()))

        val result = harness.callFilter(stcReq).futureValue

        result mustBe defined
      }
    }

    "must redirect when confidence < L250" in {
      val app = applicationBuilder().build()
      running(app) {
        val parser = app.injector.instanceOf[BodyParsers.Default]
        val redirects = app.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects]

        val itmpName = ItmpName(Some("First"), None, Some("Last"))
        val stcReq = Fixtures.fakeStcAuthRequest(request = FakeRequest(), confidenceLevelOverride = ConfidenceLevel.L50, maybeNinoOverride = Some("AA123456A"), maybeNameOverride = Some(itmpName))

        val harness = new Harness(parser, redirects, new Fixtures.FakeAuthConnectorSuccess(()))

        val result = harness.callFilter(stcReq).futureValue

        result mustBe defined
      }
    }
  }
}
