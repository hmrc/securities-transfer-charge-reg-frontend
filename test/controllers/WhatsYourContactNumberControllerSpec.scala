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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.EnrolmentResponse.EnrolmentSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionResponse.SubscriptionSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionStatus.SubscriptionActive
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.{IndividualEnrolmentDetails, IndividualSubscriptionDetails, RegistrationClient}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.WhatsYourContactNumberFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.{AddressPage, DateOfBirthRegPage, WhatsYourContactNumberPage, WhatsYourEmailAddressPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.WhatsYourContactNumberView

import java.time.LocalDate
import scala.concurrent.Future



class WhatsYourContactNumberControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new WhatsYourContactNumberFormProvider()
  val form: Form[String] = formProvider()

  lazy val whatsYourContactNumberRoute: String = routes.WhatsYourContactNumberController.onPageLoad(NormalMode).url

  "WhatsYourContactNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatsYourContactNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatsYourContactNumberView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(WhatsYourContactNumberPage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatsYourContactNumberRoute)

        val view = application.injector.instanceOf[WhatsYourContactNumberView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to RegistrationCompletePage on successful subscribe and enrol after valid data is submitted" in {

      val userAnswers =
        emptyUserAnswers
          .set(AddressPage(), fakeAddress).success.value
          .set(WhatsYourEmailAddressPage, "test@test.com").success.value
          .set(WhatsYourContactNumberPage, "07538 511 122").success.value
          .set(DateOfBirthRegPage, LocalDate.now().minusYears(20)).success.value

      val fakeRegistrationClient = mock[RegistrationClient]

      when(fakeRegistrationClient.subscribe(any[IndividualSubscriptionDetails]()))
        .thenReturn(Future.successful(Right(SubscriptionSuccessful)))

      when(fakeRegistrationClient.enrolIndividual(any[IndividualEnrolmentDetails]()))
        .thenReturn(Future.successful(Right(EnrolmentSuccessful)))

      when(fakeRegistrationClient.hasCurrentSubscription(any[String]()))
        .thenReturn(Future.successful(Right(SubscriptionActive)))


      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[RegistrationClient].toInstance(fakeRegistrationClient)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatsYourContactNumberRoute)
            .withFormUrlEncodedBody(("value", "07538 511 122"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.RegistrationCompleteController.onPageLoad().url
      }
    }


    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, whatsYourContactNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WhatsYourContactNumberView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whatsYourContactNumberRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whatsYourContactNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
