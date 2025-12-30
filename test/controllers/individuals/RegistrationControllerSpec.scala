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

package controllers.individuals

import base.Fixtures.FakeStcAuthAction
import base.SpecBase
import play.api.inject
import play.api.mvc.*
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.ItmpName
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.{Auth, StcAuthAction}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.individuals.RegistrationController
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcAuthRequest

import scala.concurrent.ExecutionContext

class RegistrationControllerSpec extends SpecBase {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global


  "RegistrationController" - {

    "should redirect organisation users to organisation registration" in {

      def fakeStcAuthRequest[A](request: Request[A]): StcAuthRequest[A]
        =new StcAuthRequest(
        request,
        "user-123",
        uk.gov.hmrc.auth.core.Enrolments(Set()),
        Organisation,
        ConfidenceLevel.L50,
        None,
        None
      )
      val authAction = FakeStcAuthAction(fakeStcAuthRequest(FakeRequest()))
      val application = applicationBuilderNoAuth()
        .overrides(inject.bind[StcAuthAction].toInstance(authAction))
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val controller = new RegistrationController(mcc, redirects, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.registerOrganisationUrl)
      }
    }

    "should redirect agents to ASA" in {
      def fakeStcAuthRequest[A](request: Request[A]): StcAuthRequest[A]
      = new StcAuthRequest(
        request,
        "user-123",
        uk.gov.hmrc.auth.core.Enrolments(Set()),
        Agent,
        ConfidenceLevel.L50,
        None,
        None
      )

      val authAction = FakeStcAuthAction(fakeStcAuthRequest(FakeRequest()))
      val application = applicationBuilderNoAuth()
        .overrides(inject.bind[StcAuthAction].toInstance(authAction))
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val controller = new RegistrationController(mcc, redirects, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.asaUrl)
      }
    }

    "should redirect individuals with insufficient confidence to IV uplift" in {
      def fakeStcAuthRequest[A](request: Request[A]): StcAuthRequest[A]
      = new StcAuthRequest(
        request,
        "user-123",
        uk.gov.hmrc.auth.core.Enrolments(Set()),
        Individual,
        ConfidenceLevel.L50,
        None,
        None
      )

      val authAction = FakeStcAuthAction(fakeStcAuthRequest(FakeRequest()))
      val application = applicationBuilderNoAuth()
        .overrides(inject.bind[StcAuthAction].toInstance(authAction))
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val controller = new RegistrationController(mcc, redirects, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.ivUpliftUrl)
      }
    }

    "should redirect individuals with sufficient confidence but no Name to IV uplift" in {
      def fakeStcAuthRequest[A](request: Request[A]): StcAuthRequest[A]
      = new StcAuthRequest(
        request,
        "user-123",
        uk.gov.hmrc.auth.core.Enrolments(Set()),
        Individual,
        ConfidenceLevel.L250,
        Some("NZ153756A"),
        None
      )

      val authAction = FakeStcAuthAction(fakeStcAuthRequest(FakeRequest()))
      val application = applicationBuilderNoAuth()
        .overrides(inject.bind[StcAuthAction].toInstance(authAction))
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val controller = new RegistrationController(mcc, redirects, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.ivUpliftUrl)
      }
    }



    "should redirect individuals with sufficient confidence and other data to individual registration" in {
      def fakeStcAuthRequest[A](request: Request[A]): StcAuthRequest[A]
      = new StcAuthRequest(
        request,
        "user-123",
        uk.gov.hmrc.auth.core.Enrolments(Set()),
        Individual,
        ConfidenceLevel.L250,
        Some("NZ153756A"),
        Some(ItmpName(Some("First"), None, Some("Last")))
      )

      val authAction = FakeStcAuthAction(fakeStcAuthRequest(FakeRequest()))
      val application = applicationBuilderNoAuth()
        .overrides(inject.bind[StcAuthAction].toInstance(authAction))
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val controller = new RegistrationController(mcc, redirects, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.registerIndividualUrl)
      }
    }
  }
}

