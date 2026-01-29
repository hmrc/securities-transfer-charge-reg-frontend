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

import base.SpecBase
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.IndividualsNavigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.{AddressPage, Page}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.{CheckYourDetailsPage, DateOfBirthRegPage, RegForSecuritiesTransferChargePage, WhatsYourEmailAddressPage}

class IndividualsNavigatorSpec extends SpecBase {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val navigator = new IndividualsNavigator(new repositories.FakeSessionRepository)

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad()
      }

      "must go from the RegForSecuritiesTransferChargePage to CheckYourDetailsPage" in {
        navigator.nextPage(RegForSecuritiesTransferChargePage, NormalMode, UserAnswers("id")) mustBe individualRoutes.CheckYourDetailsController.onPageLoad(NormalMode)
      }

      "must go from the CheckYourDetailsPage to DateOfBirthRegController when user answers Yes" in {
        val answers = emptyUserAnswers
          .set(CheckYourDetailsPage, true).success.value
        navigator.nextPage(CheckYourDetailsPage, NormalMode, answers) mustBe individualRoutes.DateOfBirthRegController.onPageLoad(NormalMode)
      }
      
      "must go from the CheckYourDetailsPage to UpdateDetailsKickOutPage when user answers No" in {
        val answers = emptyUserAnswers
          .set(CheckYourDetailsPage, false).success.value
        navigator.nextPage(CheckYourDetailsPage, NormalMode, answers) mustBe individualRoutes.UpdateDetailsKickOutController.onPageLoad()
      }

      "must go from the CheckYourDetailsPage to JourneyRecovery when user answers not defined" in {
        val answers = emptyUserAnswers
        navigator.nextPage(CheckYourDetailsPage, NormalMode, answers) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from the DateOfBirth to Address" in {
        val answers = emptyUserAnswers
        navigator.nextPage(DateOfBirthRegPage, NormalMode, answers) mustBe individualRoutes.AddressController.onPageLoad()
      }

      "must go from the Address to WhatsYourEmailAddressController" in {
        val answers = emptyUserAnswers
        navigator.nextPage(AddressPage(), NormalMode, answers) mustBe individualRoutes.WhatsYourEmailAddressController.onPageLoad(NormalMode)
      }
      
      "must go from the WhatsYourEmailAddressPage to WhatsYourContactNumberPage" in {
        navigator.nextPage(
          WhatsYourEmailAddressPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe individualRoutes.WhatsYourContactNumberController.onPageLoad(NormalMode)
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id")) mustBe routes.CheckYourAnswersController.onPageLoad()
      }
    }
  }
}
