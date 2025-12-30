package controllers.organisations

import base.SpecBase
import controllers.routes
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.RegForSecuritiesTransferChargeView

class RegForSecuritiesTransferChargeControllerSpec extends SpecBase {

  "RegForSecuritiesTransferCharge Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RegForSecuritiesTransferChargeController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RegForSecuritiesTransferChargeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
