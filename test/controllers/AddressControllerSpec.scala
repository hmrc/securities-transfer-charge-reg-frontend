// scala
package controllers

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.{AddressController, routes}

class AddressControllerSpec extends SpecBase with MockitoSugar {

  private val onwardRoute = Call("GET", "/foo")

  "AddressController" - {

    "onPageLoad should return the result from Alf.initAlfJourneyRequest" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      val controller = application.injector.instanceOf[AddressController]

      running(application) {
        val request = FakeRequest(GET, routes.AddressController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "onReturn should retrieve address from Alf, store it and redirect to the next page" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      val controller = application.injector.instanceOf[AddressController]


      running(application) {
        val request = FakeRequest(GET, routes.AddressController.onReturn("key").url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }
  }
}
