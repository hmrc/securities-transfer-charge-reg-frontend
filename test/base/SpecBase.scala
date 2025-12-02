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

import controllers.actions.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{ActionBuilder, AnyContent, AnyContentAsEmpty}
import play.api.test.{FakeRequest, Helpers}
import repositories.FakeSessionRepository
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.{DataRequest, IdentifierRequest}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{UserAnswers, UserDetails}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

class FakeStcAuthAction extends StcAuthAction {

  private val dummyUserDetails = UserDetails(
    firstName = Some("Test"),
    lastName = Some("User"),
    affinityGroup = AffinityGroup.Individual,
    confidenceLevel = ConfidenceLevel.L200,
    nino = Some("AA123456A")
  )
  override def authorise: ActionBuilder[IdentifierRequest, AnyContent] = new ActionBuilder[IdentifierRequest, AnyContent] {
    override def parser = Helpers.stubBodyParser(AnyContentAsEmpty)

    override protected def executionContext = scala.concurrent.ExecutionContext.Implicits.global

    override def invokeBlock[A](request: play.api.mvc.Request[A], block: IdentifierRequest[A] => scala.concurrent.Future[play.api.mvc.Result]) =
      block(IdentifierRequest(request, "userId", dummyUserDetails))
  }
}

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  val userAnswersId: String = "id"
  val sessionId = "sessionId1234"
  val firstName = "TestFirstName"
  val lastName = "TestLastName"
  val affinityGroup = AffinityGroup.Individual
  val confidenceLevel = ConfidenceLevel.L250
  val nino = "AB 12 34 56 C"

  val fakeUserDetails: UserDetails = UserDetails(Some(firstName), Some(lastName), affinityGroup, confidenceLevel, Some(nino))

  val fakeRequest = FakeRequest().withHeaders("sessionId" -> sessionId)

  def fakeDataRequest(userAnswers: UserAnswers): DataRequest[AnyContent] = DataRequest[AnyContent](fakeRequest, "userId", userAnswers)

  def emptyUserAnswers : UserAnswers = UserAnswers(userAnswersId)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[SessionRepository].to[FakeSessionRepository],
        bind[StcAuthAction].to[FakeStcAuthAction]
      )

  protected def applicationBuilderWithoutSessionRepository(userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[StcAuthAction].to[FakeStcAuthAction]
      )
}
