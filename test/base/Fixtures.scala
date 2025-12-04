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

import play.api.mvc.*
import play.api.mvc.request.RequestFactory
import play.api.test.{FakeRequest, FakeRequestFactory, Helpers}
import uk.gov.hmrc.auth.core.authorise.*
import uk.gov.hmrc.auth.core.retrieve.{ItmpName, Retrieval}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.StcAuthAction
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.{IdentifierRequest, StcAuthRequest}

import scala.concurrent.{ExecutionContext, Future}

object Fixtures {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val user = "user-123"
  val enrolments = Enrolments(Set())
  val affinityGroup: AffinityGroup.Individual.type = AffinityGroup.Individual
  val confidenceLevel: ConfidenceLevel = ConfidenceLevel.L250
  val maybeNino = Some("AA123456A")
  val maybeName = Some(ItmpName(Some("First"), Some("Middle"), Some("Last")))
  
  // Use the no-arg FakeRequest factory (matches other tests in the project) to avoid constructor overload issues
  val fakeIdentifierRequest = IdentifierRequest[AnyContent](FakeRequest(), user)
  val requestFactory = FakeRequestFactory(RequestFactory.plain)

  /**
   * Build a fake StcAuthRequest for tests.
   * By default it uses the fixture values (user, enrolments, affinityGroup, confidenceLevel, maybeNino, maybeName),
   * but you can pass a different Request[A] to change the body type or headers.
   */
  def fakeStcAuthRequest[A](
    request: Request[A],
    userId: String = user,
    enrolmentsOverride: Enrolments = enrolments,
    affinityGroupOverride: AffinityGroup = affinityGroup,
    confidenceLevelOverride: ConfidenceLevel = confidenceLevel,
    maybeNinoOverride: Option[String] = maybeNino,
    maybeNameOverride: Option[ItmpName] = maybeName
  ): StcAuthRequest[A] =
    StcAuthRequest[A](
      request,
      userId,
      enrolmentsOverride,
      affinityGroupOverride,
      confidenceLevelOverride,
      maybeNinoOverride,
      maybeNameOverride
    )


  // simple stub AuthConnector that returns a preconfigured value for any retrieval
  class FakeAuthConnector[T](value: T) extends AuthConnector {

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.successful(value.asInstanceOf[A])
  }

  class FakeAuthConnectorSuccess(value: Any) extends AuthConnector {

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.successful(value.asInstanceOf[A])
  }

  class FakeAuthConnectorFailing(ex: Throwable) extends AuthConnector {

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(ex)
  }
  
  // Fake StcAuthAction for tests - always invokes the provided block with the fixed StcAuthRequest.
  // Build an adapted StcAuthRequest[B] from the incoming request to avoid unsafe casting.
  class FakeStcAuthAction[A](fixedRequest: StcAuthRequest[A]) extends StcAuthAction {
    override val parser: BodyParser[AnyContent] = Helpers.stubBodyParser[AnyContent]()
    implicit val executionContext: ExecutionContext = Fixtures.ec

    override def invokeBlock[B](request: Request[B], block: StcAuthRequest[B] => Future[Result]): Future[Result] = {
      val adapted = StcAuthRequest[B](
        request,
        fixedRequest.userId,
        fixedRequest.enrolments,
        fixedRequest.affinityGroup,
        fixedRequest.confidenceLevel,
        fixedRequest.maybeNino,
        fixedRequest.maybeName
      )
      block(adapted)
    }
  }

//  val mockAuthorisedFunctions: AuthorisedFunctions = mock[AuthorisedFunctions]
//  import mockAuthorisedFunctions.*
//
//  def mockAuthorisedFunction(internalId: String, enrolments: Enrolments, affinityGroup: Option[AffinityGroup],
//                             confidenceLevel: ConfidenceLevel, nino: Option[String], itmpName: Option[ItmpName]): AuthorisedFunction = {
//    val theMock = mock[AuthorisedFunction]
//    val mockInternalIdRetrieval = mock[AuthorisedFunctionWithResult[Option[String]]]
//    when(mockInternalIdRetrieval.retrieve()).thenReturn(Future.successful(Some(internalId)))
//    when(theMock.retrieve(OptionalRetrieval("internalId", Reads.StringReads))).thenReturn(AuthorisedFunction(AuthorisedFunctionWithResult[]()))
//  }
//
//  when(mockAuthorisedFunctions.authorised()).thenReturn(mockAuthorisedFunction)

}
