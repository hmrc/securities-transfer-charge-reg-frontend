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
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.{SelectBusinessType, TypeOfPartnership}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.OrgNavigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.*

class OrgNavigatorSpec extends SpecBase with ScalaFutures {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val navigator = new OrgNavigator(new repositories.FakeSessionRepository)

  "Navigator" - {

    "in Normal mode" - {
      "must go from RegistrationStart to UkOrNot" in {
        val answers = emptyUserAnswers
        val result = navigator.nextPage(RegForSecuritiesTransferChargePage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.UkOrNotController.onPageLoad(NormalMode)
        }
      }

      "must go from UkOrNot to SelectBusinessType if the answer is yes" in {
        val answers = emptyUserAnswers.set(UkOrNotPage, true).get
        val result = navigator.nextPage(UkOrNotPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.SelectBusinessTypeController.onPageLoad(NormalMode)
        }
      }

      "must go from UkOrNot to UkOrNotKickOut if the answer is no" in {
        val answers = emptyUserAnswers.set(UkOrNotPage, false).get
        val result = navigator.nextPage(UkOrNotPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.UkOrNotKickOutController.onPageLoad()
        }
      }

      "must go from the SelectBusinessTypePage to partnershipKickOutPage when soletrader is selected" in {
        val answers = emptyUserAnswers
          .set(SelectBusinessTypePage, SelectBusinessType.SoleTrader).success.value
        val result = navigator.nextPage(SelectBusinessTypePage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.PartnershipKickOutController.onPageLoad()
        }
      }

      "must go from the SelectBusinessTypePage to TypeOfPartnershipPage when partnership is selected" in {
        val answers = emptyUserAnswers
          .set(SelectBusinessTypePage, SelectBusinessType.Partnership).success.value
        val result = navigator.nextPage(SelectBusinessTypePage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.TypeOfPartnershipController.onPageLoad(NormalMode)
        }
      }

      "must go from the SelectBusinessTypePage to limitedCompanyJourney when limited company is selected" in {
        val answers = emptyUserAnswers
          .set(SelectBusinessTypePage, SelectBusinessType.LimitedCompany).success.value
        val result = navigator.nextPage(SelectBusinessTypePage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.GrsIncorporatedEntityController.limitedCompanyJourney
        }
      }

      "must go from the SelectBusinessTypePage to registeredSocietyJourney when registered society is selected" in {
        val answers = emptyUserAnswers
          .set(SelectBusinessTypePage, SelectBusinessType.RegisteredSociety).success.value
        val result = navigator.nextPage(SelectBusinessTypePage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.GrsIncorporatedEntityController.registeredSocietyJourney
        }
      }

      "must go from the SelectBusinessTypePage to GRS trust journey when trust is selected" in {
        val answers = emptyUserAnswers
          .set(SelectBusinessTypePage, SelectBusinessType.Trust).success.value
        val result = navigator.nextPage(SelectBusinessTypePage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.GrsMinorEntityController.trustJourney
        }
      }

      "must go from the SelectBusinessTypePage to GRS unincorporated entity journey when unincorporated entity is selected" in {
        val answers = emptyUserAnswers
          .set(SelectBusinessTypePage, SelectBusinessType.UnincorporatedAssociation).success.value
        val result = navigator.nextPage(SelectBusinessTypePage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.GrsMinorEntityController.unincorporatedAssociationJourney
        }
      }
      
      "must go from the TypeOfPartnershipPage to partnershipKickOutPage when general partnership is selected" in {
        val answers = emptyUserAnswers
          .set(TypeOfPartnershipPage, TypeOfPartnership.GeneralPartnership).success.value
        val result = navigator.nextPage(TypeOfPartnershipPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.PartnershipKickOutController.onPageLoad()
        }
      }

      "must go from the TypeOfPartnershipPage to partnershipKickOutPage when scottish partnership is selected" in {
        val answers = emptyUserAnswers
          .set(TypeOfPartnershipPage, TypeOfPartnership.ScottishPartnership).success.value
        val result = navigator.nextPage(TypeOfPartnershipPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.PartnershipKickOutController.onPageLoad()
        }
      }

      "must go from the TypeOfPartnershipPage to GRS limited partnership journey when limited partnership is selected" in {
        val answers = emptyUserAnswers
          .set(TypeOfPartnershipPage, TypeOfPartnership.LimitedPartnership).success.value
        val result = navigator.nextPage(TypeOfPartnershipPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.GrsPartnershipController.limitedPartnershipJourney
        }
      }

      "must go from the TypeOfPartnershipPage to GRS Scottish limited partnership journey when Scottish limited partnership is selected" in {
        val answers = emptyUserAnswers
          .set(TypeOfPartnershipPage, TypeOfPartnership.ScottishLimitedPartnership).success.value
        val result = navigator.nextPage(TypeOfPartnershipPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.GrsPartnershipController.scottishLimitedPartnershipJourney
        }
      }

      "must go from the TypeOfPartnershipPage to GRS limited liability partnership journey when limited liability partnership is selected" in {
        val answers = emptyUserAnswers
          .set(TypeOfPartnershipPage, TypeOfPartnership.LimitedLiabilityPartnership).success.value
        val result = navigator.nextPage(TypeOfPartnershipPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.GrsPartnershipController.limitedLiabilityPartnershipJourney
        }
      }
      
      "must go from the ContactEmailAddressPage to ContactNumberPage" in {
        val answers = emptyUserAnswers
          .set(ContactEmailAddressPage, "foo@example.com").success.value
        val result = navigator.nextPage(ContactEmailAddressPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe orgRoutes.ContactNumberController.onPageLoad(NormalMode)
        }
      }

      "previousPage" - {

        "must go from UkOrNotPage back to RegForSecuritiesTransferChargePage" in {
          val result = navigator.previousPage(UkOrNotPage, NormalMode)

          result mustBe orgRoutes.RegForSecuritiesTransferChargeController.onPageLoad()
        }

        "must go from SelectBusinessTypePage back to UkOrNotPage" in {
          val result = navigator.previousPage(SelectBusinessTypePage, NormalMode)

          result mustBe orgRoutes.UkOrNotController.onPageLoad(NormalMode)
        }

        "must go from TypeOfPartnershipPage back to SelectBusinessTypePage" in {
          val result = navigator.previousPage(TypeOfPartnershipPage, NormalMode)

          result mustBe orgRoutes.SelectBusinessTypeController.onPageLoad(NormalMode)
        }

        "must go from ContactNumberPage back to ContactEmailAddressPage" in {
          val result = navigator.previousPage(ContactNumberPage, NormalMode)

          result mustBe orgRoutes.ContactEmailAddressController.onPageLoad(NormalMode)
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
          val result = navigator.previousPage(UkOrNotPage, CheckMode)

          result mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }
    }
  }
}
