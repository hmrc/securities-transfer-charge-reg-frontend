/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.organisations

import base.SpecBase
import navigation.FakeNavigator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations.TypeOfPartnershipFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.TypeOfPartnership
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.TypeOfPartnershipPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.TypeOfPartnershipView

import scala.concurrent.Future

class TypeOfPartnershipControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val typeOfPartnershipRoute: String = routes.TypeOfPartnershipController.onPageLoad(NormalMode).url
  val backLinkRoute: Call = routes.SelectBusinessTypeController.onPageLoad(NormalMode)
  
  val formProvider = new TypeOfPartnershipFormProvider()
  val form: Form[TypeOfPartnership] = formProvider()

  "TypeOfPartnership Controller" - {

    "must return OK and correctly load the TypeOfPartnership page" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, typeOfPartnershipRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TypeOfPartnershipView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, backLinkRoute)(request, messages(application)).toString
      }
    }

    "on pageLoad must populate the TypeOfPartnership page correctly when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(TypeOfPartnershipPage, TypeOfPartnership.GeneralPartnership).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, typeOfPartnershipRoute)

        val view = application.injector.instanceOf[TypeOfPartnershipView]

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form.fill(TypeOfPartnership.GeneralPartnership), NormalMode, backLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith("organisations").toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, typeOfPartnershipRoute)
            .withFormUrlEncodedBody(("value", TypeOfPartnership.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, typeOfPartnershipRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[TypeOfPartnershipView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, backLinkRoute)(request, messages(application)).toString
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, typeOfPartnershipRoute)
            .withFormUrlEncodedBody(("value", TypeOfPartnership.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
