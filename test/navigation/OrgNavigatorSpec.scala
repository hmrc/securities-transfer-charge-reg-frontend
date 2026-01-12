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
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.{SelectBusinessType, TypeOfPartnership}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.OrgNavigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.{ContactEmailAddressPage, SelectBusinessTypePage, TypeOfPartnershipPage}

class OrgNavigatorSpec extends SpecBase {

  val navigator = new OrgNavigator

  "Navigator" - {

    "in Normal mode" - {

      "must go from the TypeOfPartnershipPage to partnershipKickOutPage when general partnership is selected" in {
        val answers = emptyUserAnswers
          .set(TypeOfPartnershipPage, TypeOfPartnership.GeneralPartnership).success.value
        navigator.nextPage(
          TypeOfPartnershipPage,
          NormalMode,
          answers) mustBe orgRoutes.PartnershipKickOutController.onPageLoad()
      }

      "must go from the TypeOfPartnershipPage to partnershipKickOutPage when scottish partnership is selected" in {
        val answers = emptyUserAnswers
          .set(TypeOfPartnershipPage, TypeOfPartnership.ScottishPartnership).success.value
        navigator.nextPage(
          TypeOfPartnershipPage,
          NormalMode,
          answers) mustBe orgRoutes.PartnershipKickOutController.onPageLoad()
      }

      "must go from the SelectBusinessTypePage to partnershipKickOutPage when soletrader is selected" in {
        val answers = emptyUserAnswers
          .set(SelectBusinessTypePage, SelectBusinessType.SoleTrader).success.value
        navigator.nextPage(
          SelectBusinessTypePage,
          NormalMode,
          answers) mustBe orgRoutes.PartnershipKickOutController.onPageLoad()
      }

      "must go from the SelectBusinessTypePage to TypeOfPartnershipPage when partnership is selected" in {
        val answers = emptyUserAnswers
          .set(SelectBusinessTypePage, SelectBusinessType.Partnership).success.value
        navigator.nextPage(
          SelectBusinessTypePage,
          NormalMode,
          answers) mustBe orgRoutes.TypeOfPartnershipController.onPageLoad(NormalMode)
      }

      "must go from the ContactEmailAddressPage to ContactNumberPage" in {
        val answers = emptyUserAnswers
          .set(ContactEmailAddressPage, "foo@example.com").success.value
        navigator.nextPage(
          ContactEmailAddressPage,
          NormalMode,
          answers) mustBe orgRoutes.ContactNumberController.onPageLoad(NormalMode)
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
