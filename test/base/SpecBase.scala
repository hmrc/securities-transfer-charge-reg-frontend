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

import base.Fixtures.FakeAlfConnector
import connectors.AlfAddressConnector
import controllers.actions.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.FakeRequest
import repositories.FakeSessionRepository
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.{DataRequest, StcAuthRequest}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{AlfAddress, AlfConfirmedAddress, Country, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeStcAuthAction @Inject()(bodyParsers: PlayBodyParsers) extends StcAuthAction {

  override def parser: BodyParser[AnyContent] = bodyParsers.default

  override def invokeBlock[A](request: Request[A], block: StcAuthRequest[A] => Future[Result]): Future[Result] = block(Fixtures.fakeStcAuthRequest(request))

  override protected def executionContext: ExecutionContext = ExecutionContext.global
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
  val firstName = "First"
  val lastName = "Last"
  val nino = "AA123456A"
  val affinityGroup = AffinityGroup.Individual
  val confidenceLevel = ConfidenceLevel.L250

  val fakeAddress = AlfConfirmedAddress(
    auditRef = "ref",
    id = Some("id"),
    address = AlfAddress(
      lines = List(
        "1 Test House",
        "Test Street",
        "Test City"
      ),
      postcode = "ZZ1 1ZZ",
      country = Country("GB", "United Kingdom")
    )
  )


  val fakeRequest = FakeRequest().withHeaders("sessionId" -> sessionId)

  def fakeDataRequest(userAnswers: UserAnswers): DataRequest[AnyContent]
    = DataRequest[AnyContent](Fixtures.fakeStcAuthRequest(FakeRequest()), "userId", userAnswers)

  def emptyUserAnswers : UserAnswers = UserAnswers(userAnswersId)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[StcAuthAction].to[FakeStcAuthAction],
        bind[AlfAddressConnector].to[FakeAlfConnector]
      )

  protected def applicationBuilderNoAuth(userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[SessionRepository].to[FakeSessionRepository],
        bind[AlfAddressConnector].to[FakeAlfConnector]
      )

  protected def applicationBuilderWithoutSessionRepository(userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[StcAuthAction].to[FakeStcAuthAction],
        bind[AlfAddressConnector].to[FakeAlfConnector]

      )
}
