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
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.{AddressPage, Page, individuals as individualsPages}

import javax.inject.{Inject, Singleton}

@Singleton
class IndividualsNavigator @Inject() extends Navigator {

  private val normalRoutes: Page => UserAnswers => Call = {
    case individualsPages.RegForSecuritiesTransferChargePage =>
      _ => individualRoutes.CheckYourDetailsController.onPageLoad(NormalMode)

    case individualsPages.CheckYourDetailsPage =>
      userAnswers =>
        userAnswers.get(individualsPages.CheckYourDetailsPage) match {
          case Some(true)  => individualRoutes.DateOfBirthRegController.onPageLoad(NormalMode)
          case Some(false) => individualRoutes.UpdateDetailsKickOutController.onPageLoad()
          case None        => routes.JourneyRecoveryController.onPageLoad()
        }

    case individualsPages.DateOfBirthRegPage =>
      userAnswers =>
        userAnswers.get(individualsPages.CheckYourDetailsPage) match {
          case Some(_)  => individualRoutes.AddressController.onPageLoad()
          case None     => individualRoutes.UpdateDobKickOutController.onPageLoad()
        }
      
    case _: AddressPage[_] =>
      _ => individualRoutes.WhatsYourEmailAddressController.onPageLoad(NormalMode)

    case individualsPages.WhatsYourEmailAddressPage =>
      _ => individualRoutes.WhatsYourContactNumberController.onPageLoad(NormalMode)

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
