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
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.CheckYourDetailsFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.CheckYourDetailsPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.CheckYourDetailsView

class CheckYourDetailsControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new CheckYourDetailsFormProvider()
  private val form = formProvider()

  lazy val checkYourDetailsRoute =
    routes.CheckYourDetailsController.onPageLoad(NormalMode).url

  "CheckYourDetailsController" - {

    "must return OK and the correct view for a GET" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, checkYourDetailsRoute)

        val result = route(application, request).value
        val view   = application.injector.instanceOf[CheckYourDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, firstName, lastName, nino, NormalMode)(
            request,
            messages(application)
          ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers =
        UserAnswers(userAnswersId)
          .set(CheckYourDetailsPage, true).success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, checkYourDetailsRoute)
        val result = route(application, request).value
        val view   = application.injector.instanceOf[CheckYourDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form.fill(true), firstName, lastName, nino, NormalMode)(
            request,
            messages(application)
          ).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, checkYourDetailsRoute)
            .withFormUrlEncodedBody("value" -> "")

        val boundForm = form.bind(Map("value" -> ""))
        val result = route(application, request).value
        val view   = application.injector.instanceOf[CheckYourDetailsView]

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual
          view(boundForm, firstName, lastName, nino, NormalMode)(
            request,
            messages(application)
          ).toString
      }
    }
  }
}
