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

package uk.gov.hmrc.securitiestransferchargeregfrontend.navigation

import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.individuals.{routes => individualRoutes}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations.{routes => orgRoutes}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.{CheckYourDetailsPage, DateOfBirthRegPage, WhatsYourEmailAddressPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.UkOrNotPage

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject()() {

  private val normalRoutes: Page => UserAnswers => Call = {
    case individuals.RegForSecuritiesTransferChargePage =>
      _ => individualRoutes.CheckYourDetailsController.onPageLoad(NormalMode)

    case CheckYourDetailsPage =>
      userAnswers =>
        userAnswers.get(CheckYourDetailsPage) match {
          case Some(true)  => individualRoutes.DateOfBirthRegController.onPageLoad(NormalMode)
          case Some(false) => individualRoutes.UpdateDetailsKickOutController.onPageLoad()
          case None        => routes.JourneyRecoveryController.onPageLoad()
        }

    case DateOfBirthRegPage =>
      _ => routes.AddressController.onPageLoad()

    case _: AddressPage[_] =>
      _ => individualRoutes.WhatsYourEmailAddressController.onPageLoad(NormalMode)

    case WhatsYourEmailAddressPage =>
      _ => individualRoutes.WhatsYourContactNumberController.onPageLoad(NormalMode)

    case organisations.RegForSecuritiesTransferChargePage =>
      _ => orgRoutes.UkOrNotController.onPageLoad(NormalMode)

    case UkOrNotPage =>
      userAnswers => {
        userAnswers.get(UkOrNotPage) match {
          case Some(true) => ???
          case Some(false) => orgRoutes.UkOrNotKickOutController.onPageLoad()
          case None => routes.JourneyRecoveryController.onPageLoad()
        }
      }

    case _ =>
      _ => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = (_ => _ => routes.CheckYourAnswersController.onPageLoad())

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call =
    mode match {
      case NormalMode => normalRoutes(page)(userAnswers)
      case CheckMode  => checkRouteMap(page)(userAnswers)
    }
}
