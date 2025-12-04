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

package controllers

import base.Fixtures.FakeAuthConnector
import base.SpecBase
import play.api.mvc.*
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.{ItmpName, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.Auth
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.{Redirects, RegistrationController}

import scala.concurrent.ExecutionContext

class RegistrationControllerSpec extends SpecBase {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def buildRetrieval(affinityGroup: AffinityGroup, confidenceLevel: ConfidenceLevel, nino: Option[String], itmpName: Option[ItmpName])
    : Option[AffinityGroup] ~ ConfidenceLevel ~ Option[String] ~ Option[ItmpName] = {
    new ~(
      new ~(
        new ~(Some(affinityGroup), confidenceLevel), nino), itmpName)
  }

  "RegistrationController" - {

    "should redirect organisation users to organisation registration" in {
      val application = applicationBuilder()
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val retrievalValue = buildRetrieval(Organisation, ConfidenceLevel.L50, None, None)

        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.registerOrganisationUrl)
      }
    }

    "should redirect agents to ASA" in {
      val application = applicationBuilder()
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val retrievalValue = buildRetrieval(Agent, ConfidenceLevel.L50, None, None)
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.asaUrl)
      }
    }

    "should redirect individuals with insufficient confidence to IV uplift" in {
      val application = applicationBuilder()
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L50, None, None)
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.ivUpliftUrl)
      }
    }

    "should redirect individuals with sufficient confidence but no NINO IV uplift" in {
      val application = applicationBuilder()
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L250, None, Some(ItmpName(Some("First"), None, Some("Last"))))
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.ivUpliftUrl)
      }
    }

    "should redirect individuals with sufficient confidence but no name to IV Uplift" in {
      val application = applicationBuilder()
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L250, Some("NZ153756A"), None)
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.ivUpliftUrl)
      }
    }

    "should redirect individuals with sufficient confidence and other data to individual registration" in {
      val application = applicationBuilder()
        .configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]
        val auth = application.injector.instanceOf[Auth]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L250, Some("NZ153756A"), Some(ItmpName(Some("First"), None, Some("Last"))))
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, auth)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.registerIndividualUrl)
      }
    }
  }
}

