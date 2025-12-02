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

import base.SpecBase
import play.api.mvc.*
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ItmpName, Retrieval, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.{IdentifierAction, StcAuthAction}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.{Redirects, RegistrationController}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.*

import scala.concurrent.{ExecutionContext, Future}

class RegistrationControllerSpec extends SpecBase {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  // simple stub AuthConnector that returns a preconfigured value for any retrieval
  class FakeAuthConnector[T](value: T) extends AuthConnector {
    val serviceUrl: String = ""
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.successful(value.asInstanceOf[A])
  }

  // Simple pass-through IdentifierAction used instead of the real AuthenticatedIdentifierAction
  class TestIdentifierAction extends IdentifierAction {
    override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
      block(IdentifierRequest(request, "test-internal-id"))

    override def parser: BodyParser[AnyContent] = Helpers.stubBodyParser(AnyContentAsEmpty)

    override protected def executionContext: ExecutionContext = ec
  }

  // Pass-through EnrolmentCheck that just invokes the block
  class PassThroughStcAuthAction extends StcAuthAction {
    override def authorise: ActionBuilder[IdentifierRequest, AnyContent] = new ActionBuilder[IdentifierRequest, AnyContent] {
      override def parser = Helpers.stubBodyParser(AnyContentAsEmpty)

      override protected def executionContext = ec

      override def invokeBlock[A](request: play.api.mvc.Request[A], block: IdentifierRequest[A] => Future[play.api.mvc.Result]) =
        block(IdentifierRequest(request, "userId"))
    }
  }

  def buildRetrieval(affinityGroup: AffinityGroup, confidenceLevel: ConfidenceLevel, nino: Option[String], itmpName: Option[ItmpName])
    : Option[AffinityGroup] ~ ConfidenceLevel ~ Option[String] ~ Option[ItmpName] = {
    new ~(
      new ~(
        new ~(Some(affinityGroup), confidenceLevel), nino), itmpName)
  }

  "RegistrationController" - {

    "should redirect organisation users to organisation registration" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Organisation, ConfidenceLevel.L50, None, None)

        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        //val identifierAction = new TestIdentifierAction
        val authAction = new PassThroughStcAuthAction

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, authAction)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.registerOrganisationUrl)
      }
    }

    "should redirect agents to ASA" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Agent, ConfidenceLevel.L50, None, None)
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val authAction = new PassThroughStcAuthAction

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, authAction)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.asaUrl)
      }
    }

    "should redirect individuals with insufficient confidence to IV uplift" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L50, None, None)
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val authAction = new PassThroughStcAuthAction

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, authAction)

        val result = controller.routingLogic.apply(FakeRequest())
        val expected = appConfig.ivUpliftUrl

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(expected)
      }
    }

    "should redirect individuals with sufficient confidence but no NINO IV uplift" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L250, None, Some(ItmpName(Some("First"), None, Some("Last"))))
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val authAction = new PassThroughStcAuthAction

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, authAction)

        val result = controller.routingLogic.apply(FakeRequest())
        val expected = appConfig.ivUpliftUrl

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(expected)
      }
    }

    "should redirect individuals with sufficient confidence but no name to IV Uplift" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L250, Some("NZ153756A"), None)
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val authAction = new PassThroughStcAuthAction

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, authAction)

        val result = controller.routingLogic.apply(FakeRequest())
        val expected = appConfig.ivUpliftUrl

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(expected)
      }
    }

    "should redirect individuals with sufficient confidence and other data to individual registration" in {
      val application = applicationBuilder().configure().build()

      running(application) {
        val mcc = application.injector.instanceOf[MessagesControllerComponents]
        val appConfig = application.injector.instanceOf[uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig]
        val redirects = application.injector.instanceOf[Redirects]

        val retrievalValue = buildRetrieval(Individual, ConfidenceLevel.L250, Some("NZ153756A"), Some(ItmpName(Some("First"), None, Some("Last"))))
        val authConnectorForController = new FakeAuthConnector(retrievalValue)

        val authAction = new PassThroughStcAuthAction

        val controller = new RegistrationController(mcc, redirects, authConnectorForController, authAction)

        val result = controller.routingLogic.apply(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.registerIndividualUrl)
      }
    }
  }
}

