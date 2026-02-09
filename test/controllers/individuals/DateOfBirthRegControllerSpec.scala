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

import base.Fixtures.safeId
import base.SpecBase
import navigation.FakeNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.{IndividualRegistrationDetails, RegistrationClient, RegistrationResponse}
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.RegistrationConnector
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals.DateOfBirthRegFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualData
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.DateOfBirthRegPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.{RegistrationDataRepository, SessionRepository}
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.individuals.DateOfBirthRegView

import java.time.LocalDate
import scala.concurrent.Future

class DateOfBirthRegControllerSpec extends SpecBase with MockitoSugar with ScalaFutures {
  private implicit val messages: Messages = stubMessages()

  private val formProvider = new DateOfBirthRegFormProvider()
  private def form = formProvider()

  def onwardRoute = Call("GET", "/foo")

  val validAnswer: LocalDate = LocalDate.of(1990, 1, 1)
  val success: Future[Unit] = Future.successful(())
  
  lazy val dateOfBirthRegRoute: String = individualRoutes.DateOfBirthRegController.onPageLoad(NormalMode).url
  lazy val dateOfBirthPostRoute: String = individualRoutes.DateOfBirthRegController.onSubmit(NormalMode).url

  override val emptyUserAnswers = UserAnswers(userAnswersId)

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, dateOfBirthRegRoute).withSession("sessionId" -> userAnswersId)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, dateOfBirthPostRoute)
      .withFormUrlEncodedBody(
        "value.day"   -> validAnswer.getDayOfMonth.toString,
        "value.month" -> validAnswer.getMonthValue.toString,
        "value.year"  -> validAnswer.getYear.toString
      )
      .withSession("sessionId" -> userAnswersId)

  "DateOfBirthReg Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result = route(application, getRequest()).value
        val view   = application.injector.instanceOf[DateOfBirthRegView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(getRequest(), messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId).set(DateOfBirthRegPage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val view   = application.injector.instanceOf[DateOfBirthRegView]
        val result = route(application, getRequest()).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode)(getRequest(), messages(application)).toString
      }
    }

    "must clear the safe-id from the repository on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId).set(DateOfBirthRegPage, validAnswer).success.value
      val mockRegistrationConnector = mock[RegistrationConnector]
      when(mockRegistrationConnector.clearRegistration(userAnswersId)).thenReturn(success)
      
      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

      running(application) {
        val result = route(application, getRequest()).value
        whenReady(result) { _ =>
          verify(mockRegistrationConnector, times(1)).clearRegistration(any[String])
        }
      }
    }

    "must redirect to the next page and save the safe-id in the repository when valid data is submitted" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      
      val mockRegistrationConnector = mock[RegistrationConnector]
      when(mockRegistrationConnector.registerIndividual(any[String])(any[ValidIndividualData])(any[String])(any[HeaderCarrier])).thenReturn(success)
      
      val fakeRegistrationClient = mock[RegistrationClient]
      when(fakeRegistrationClient.register(any[IndividualRegistrationDetails]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(Right(RegistrationResponse.RegistrationSuccessful(safeId))))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith("individuals").toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[RegistrationClient].toInstance(fakeRegistrationClient),
            bind[RegistrationDataRepository].toInstance(new repositories.FakeRegistrationDataRepository),
            bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

      running(application) {
        val result = route(application, postRequest()).value
        whenReady(result) { _ =>
          verify(mockRegistrationConnector, times(1)).registerIndividual(any[String])(any[ValidIndividualData])(any[String])(any[HeaderCarrier])
        }
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val request =
        FakeRequest(POST, dateOfBirthPostRoute)
          .withFormUrlEncodedBody("value" -> "invalid value")
          .withSession("sessionId" -> userAnswersId)

      running(application) {
        val boundForm = form.bind(Map("value" -> "invalid value"))
        val view      = application.injector.instanceOf[DateOfBirthRegView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, getRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, postRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the KO page if registration fails" in {
      val fakeRegistrationClient = mock[RegistrationClient]
      
      when(fakeRegistrationClient.register(any[IndividualRegistrationDetails]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(Right(RegistrationResponse.RegistrationFailed)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[RegistrationClient].toInstance(fakeRegistrationClient),
            bind[RegistrationDataRepository].toInstance(new repositories.FakeRegistrationDataRepository)
          )
          .build()

      running(application) {
        val result = route(application, postRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual individualRoutes.UpdateDobKickOutController.onPageLoad().url
      }
    }
  }
}
