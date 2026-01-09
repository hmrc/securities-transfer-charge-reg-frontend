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
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.SelectBusinessType
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.TypeOfPartnership.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.{organisations as organisationsPages, *}

import javax.inject.{Inject, Singleton}

@Singleton
class OrgNavigator @Inject() extends Navigator {

  private val normalRoutes: Page => UserAnswers => Call = {
    
    case _: AddressPage[_] =>
      _ => ???

    case organisationsPages.RegForSecuritiesTransferChargePage =>
      _ => orgRoutes.UkOrNotController.onPageLoad(NormalMode)

    case organisationsPages.UkOrNotPage =>
      userAnswers => {
        userAnswers.get(organisationsPages.UkOrNotPage) match {
          case Some(true) => orgRoutes.SelectBusinessTypeController.onPageLoad(NormalMode)
          case Some(false) => orgRoutes.UkOrNotKickOutController.onPageLoad()
          case None => routes.JourneyRecoveryController.onPageLoad()
        }
      }

    case organisationsPages.SelectBusinessTypePage =>
      userAnswers => {
        userAnswers.get(organisationsPages.SelectBusinessTypePage) match {
          case Some(SelectBusinessType.Partnership) => orgRoutes.TypeOfPartnershipController.onPageLoad(NormalMode)
          case Some(SelectBusinessType.SoleTrader) => orgRoutes.PartnershipKickOutController.onPageLoad()
          case Some(_) => orgRoutes.AddressController.onPageLoad()
          case None => routes.JourneyRecoveryController.onPageLoad()
        }
      }
  
    case organisationsPages.TypeOfPartnershipPage =>
      userAnswers => typeOfPartnershipNavigation(userAnswers)

    case _ =>
      _ => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = (_ => _ => routes.CheckYourAnswersController.onPageLoad())

  private def typeOfPartnershipNavigation(userAnswers: UserAnswers): Call =
    userAnswers
      .get(organisationsPages.TypeOfPartnershipPage)
      .map {
        case GeneralPartnership | ScottishPartnership => orgRoutes.PartnershipKickOutController.onPageLoad()
        case ScottishLimitedPartnership | LimitedPartnership | LimitedLiabilityPartnership => orgRoutes.AddressController.onPageLoad()
      }
      .getOrElse(routes.JourneyRecoveryController.onPageLoad())


  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call =
    mode match {
      case NormalMode => normalRoutes(page)(userAnswers)
      case CheckMode  => checkRouteMap(page)(userAnswers)
    }
}
