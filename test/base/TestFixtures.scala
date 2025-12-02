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

package base

import play.api.mvc.{ActionBuilder, AnyContent, AnyContentAsEmpty, BodyParser, Request, Result}
import play.api.test.Helpers
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.RegistrationResponse.RegistrationSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionResponse.SubscriptionSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionStatus.SubscriptionActive
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.{IdentifierAction, StcAuthAction}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.IdentifierRequest

import scala.concurrent.{ExecutionContext, Future}

object TestFixtures {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  
  val registrationClient: RegistrationClient = new RegistrationClient {

    override def hasCurrentSubscription(etmpSafeId: String): SubscriptionStatusResult = Right(SubscriptionActive)

    override def register(individualRegistrationDetails: IndividualRegistrationDetails): RegistrationResult = Right(RegistrationSuccessful)

    override def subscribe(individualSubscriptionDetails: IndividualSubscriptionDetails): SubscriptionResult = Right(SubscriptionSuccessful)

    override def subscribe(organisationSubscriptionDetails: OrganisationSubscriptionDetails): SubscriptionResult = Right(SubscriptionSuccessful)
  }

  class FakeSuccessAuthConnector[A](value: A) extends AuthConnector {
    val serviceUrl: String = ""

    override def authorise[T](predicate: Predicate, retrieval: Retrieval[T])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[T] =
      Future.successful(value.asInstanceOf[T])
  }

  class FakeFailingAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {
    val serviceUrl: String = ""

    override def authorise[T](predicate: Predicate, retrieval: Retrieval[T])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[T] =
      Future.failed(exceptionToReturn)
  }

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
}
