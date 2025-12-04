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

import base.TestFixtures.FakeSuccessAuthConnector
import base.{SpecBase, TestFixtures}
import play.api.mvc.BodyParsers.Default
import play.api.mvc.{AnyContent, Request, Results}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.EnrolmentCheckImpl
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.IdentifierRequest

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentCheckSpec extends SpecBase {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "EnrolmentCheck" - {

    "redirect to service when user is enrolled and has current subscription" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[Default]
        val appConfig   = application.injector.instanceOf[FrontendAppConfig]
        val redirects   = application.injector.instanceOf[Redirects]

        // create an enrolment that matches the configured key
        val enrolment = Enrolment(appConfig.stcEnrolmentKey, Seq(EnrolmentIdentifier("id", "1")), "Activated")
        val enrolments = Enrolments(Set(enrolment))

        val authConnector = new FakeSuccessAuthConnector[Enrolments](enrolments)

        val registrationClient = TestFixtures.registrationClient

        val action = new EnrolmentCheckImpl(bodyParsers, appConfig, redirects, registrationClient, authConnector)(ec)

        val request = IdentifierRequest[AnyContent](FakeRequest(), "bobbins")
        val result = action.invokeBlock(request, (_: Request[Any]) => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.stcServiceUrl)
      }
    }

    "pass through when user is not enrolled" in {
      val application = applicationBuilder()
        .configure()
        .build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[Default]
        val appConfig   = application.injector.instanceOf[FrontendAppConfig]
        val redirects   = application.injector.instanceOf[Redirects]

        val enrolments = Enrolments(Set.empty)
        val authConnector = new FakeSuccessAuthConnector[Enrolments](enrolments)

        val registrationClient = TestFixtures.registrationClient
        
        val action = new EnrolmentCheckImpl(bodyParsers, appConfig, redirects, registrationClient, authConnector)(ec)

        val request = IdentifierRequest[AnyContent](FakeRequest(), "bobbins")
        val result = action.invokeBlock(request, (_: play.api.mvc.Request[Any]) => Future.successful(Results.Ok))

        status(result) mustBe OK
      }
    }

    "redirect to login on other failures" in {
      val application = applicationBuilder()
        .configure()
        .build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[Default]
        val appConfig   = application.injector.instanceOf[FrontendAppConfig]
        val redirects   = application.injector.instanceOf[Redirects]

        val authConnector = new FakeFailingAuthConnector(new MissingBearerToken)

        val registrationClient = TestFixtures.registrationClient


        val action = new EnrolmentCheckImpl(bodyParsers, appConfig, redirects, registrationClient, authConnector)(ec)

        val request = IdentifierRequest[AnyContent](FakeRequest(), "bobbins")
        val result = action.invokeBlock(request, (_: play.api.mvc.Request[Any]) => Future.successful(Results.Ok))
        val expected = appConfig.unauthorisedUrl

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(expected)
      }
    }
  }
}
