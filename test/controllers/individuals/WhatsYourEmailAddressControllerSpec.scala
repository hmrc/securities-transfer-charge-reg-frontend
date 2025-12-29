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

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.individuals.{routes => individualRoutes}
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals.WhatsYourEmailAddressFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.WhatsYourEmailAddressPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.individuals.WhatsYourEmailAddressView

class WhatsYourEmailAddressControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new WhatsYourEmailAddressFormProvider()
  val form: Form[String] = formProvider()

  lazy val whatsYourEmailAddressRoute: String = individualRoutes.WhatsYourEmailAddressController.onPageLoad(NormalMode).url

  "WhatsYourEmailAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatsYourEmailAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatsYourEmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(WhatsYourEmailAddressPage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatsYourEmailAddressRoute)

        val view = application.injector.instanceOf[WhatsYourEmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(POST, whatsYourEmailAddressRoute)
            .withFormUrlEncodedBody("value" -> "")

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val body = contentAsString(result)

        body must include(messages(application)("whatsYourEmailAddress.error.required"))
        body must include("""aria-describedby=""")
        body must include("""govuk-error-summary""")
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whatsYourEmailAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whatsYourEmailAddressRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
