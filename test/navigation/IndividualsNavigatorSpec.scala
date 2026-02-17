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

package navigation

import base.{Fixtures, SpecBase}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.IndividualsNavigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.{CheckYourDetailsPage, DateOfBirthRegPage, IndividualAddressPage, RegForSecuritiesTransferChargePage, WhatsYourContactNumberPage, WhatsYourEmailAddressPage}

import java.time.LocalDate

class IndividualsNavigatorSpec extends SpecBase {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val navigator = new IndividualsNavigator(new repositories.FakeSessionRepository)

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Journey Recovery" in {
        case object UnknownPage extends Page
        val result = navigator.nextPage(UnknownPage, NormalMode, emptyUserAnswers)
        whenReady(result) { res =>
          res mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "must go from the RegForSecuritiesTransferChargePage to CheckYourDetailsPage" in {
        val result = navigator.nextPage(RegForSecuritiesTransferChargePage, NormalMode, emptyUserAnswers)
        whenReady(result) { res =>
          res mustBe individualRoutes.CheckYourDetailsController.onPageLoad(NormalMode)
        }
      }

      "must go from the CheckYourDetailsPage to DateOfBirthRegController when user answers Yes" in {
        val answers = emptyUserAnswers.set(CheckYourDetailsPage, true).success.value
        val result = navigator.nextPage(CheckYourDetailsPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualRoutes.DateOfBirthRegController.onPageLoad(NormalMode)
        }
      }
      
      "must go from the CheckYourDetailsPage to UpdateDetailsKickOutPage when user answers No" in {
        val answers = emptyUserAnswers.set(CheckYourDetailsPage, false).success.value
        val result = navigator.nextPage(CheckYourDetailsPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualRoutes.UpdateDetailsKickOutController.onPageLoad()
        }
      }
      "must go from the CheckYourDetailsPage to JourneyRecovery when user answers not defined" in {
        val result = navigator.nextPage(CheckYourDetailsPage, NormalMode, emptyUserAnswers)
        whenReady(result) { res =>
          res mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "must go from the DateOfBirth to Address" in {
        val dob = LocalDate.now().minusYears(30)
        val answers = emptyUserAnswers.set(DateOfBirthRegPage, dob).get
        val result = navigator.nextPage(DateOfBirthRegPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualRoutes.AddressController.onPageLoad()
        }
      }

      "must go from the Address to WhatsYourEmailAddressController" in {
        val answers = emptyUserAnswers.set(IndividualAddressPage, Fixtures.fakeAlfConfirmedAddress).get
        val result = navigator.nextPage(IndividualAddressPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualRoutes.WhatsYourEmailAddressController.onPageLoad(NormalMode)
        }
      }
      
      "must go from the WhatsYourEmailAddressPage to WhatsYourContactNumberPage" in {
        val answers = emptyUserAnswers.set(WhatsYourEmailAddressPage, "foo@bar.com").get
        val result = navigator.nextPage(WhatsYourEmailAddressPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualRoutes.WhatsYourContactNumberController.onPageLoad(NormalMode)
        }
      }
      "must go from the WhatsYourContactNumberPage to RegistrationComplete" in {
        val answers = emptyUserAnswers.set(WhatsYourContactNumberPage, "01234567890").get
        val result = navigator.nextPage(WhatsYourContactNumberPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualRoutes.RegistrationCompleteController.onPageLoad()
        }
      }

      "previousPage" - {

        "must go from CheckYourDetailsPage back to RegForSecuritiesTransferChargePage" in {
          val result = navigator.previousPage(CheckYourDetailsPage, NormalMode)

          result mustBe individualRoutes.RegForSecuritiesTransferChargeController.onPageLoad()
        }

        "must go from DateOfBirthRegPage back to CheckYourDetailsPage" in {
          val result = navigator.previousPage(DateOfBirthRegPage, NormalMode)

          result mustBe individualRoutes.CheckYourDetailsController.onPageLoad(NormalMode)
        }

        "must go from WhatsYourContactNumberPage back to WhatsYourEmailAddressPage" in {
          val result = navigator.previousPage(WhatsYourContactNumberPage, NormalMode)

          result mustBe individualRoutes.WhatsYourEmailAddressController.onPageLoad(NormalMode)
        }
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        val result = navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id"))
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
        
      }

      "previousPage" - {

        "must go to CheckYourAnswers" in {
          val result = navigator.previousPage(DateOfBirthRegPage, CheckMode)

          result mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }
    }
  }
}
