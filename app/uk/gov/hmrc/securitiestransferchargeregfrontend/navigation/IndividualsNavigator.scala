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
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.{Page, individuals as individualsPages}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IndividualsNavigator @Inject()(sessionRepository: SessionRepository)
                                    (implicit ec: ExecutionContext) extends AbstractNavigator(sessionRepository) {

  private val normalRoutes: Page => UserAnswers => Future[Call] = {
    case individualsPages.RegForSecuritiesTransferChargePage =>
      userAnswers => goTo(individualRoutes.CheckYourDetailsController.onPageLoad(NormalMode), Some(userAnswers))

    case individualsPages.CheckYourDetailsPage =>
      userAnswers => dataDependent(individualsPages.CheckYourDetailsPage, userAnswers) { detailsCorrect =>
        if (detailsCorrect) individualRoutes.DateOfBirthRegController.onPageLoad(NormalMode)
        else individualRoutes.UpdateDetailsKickOutController.onPageLoad()
      }

    case individualsPages.DateOfBirthRegPage =>
      userAnswers => dataRequired(individualsPages.DateOfBirthRegPage, userAnswers, individualRoutes.AddressController.onPageLoad())
      
    case individualsPages.IndividualAddressPage =>
      userAnswers => dataRequired(individualsPages.IndividualAddressPage, userAnswers, individualRoutes.WhatsYourEmailAddressController.onPageLoad(NormalMode))

    case individualsPages.WhatsYourEmailAddressPage =>
      userAnswers => dataRequired(individualsPages.WhatsYourEmailAddressPage, userAnswers, individualRoutes.WhatsYourContactNumberController.onPageLoad(NormalMode))
      
    case individualsPages.WhatsYourContactNumberPage =>
      userAnswers => dataRequired(individualsPages.WhatsYourContactNumberPage, userAnswers, individualRoutes.RegistrationCompleteController.onPageLoad())
      
    case _ => _ => defaultPage
  }

  private val checkRouteMap: Page => UserAnswers => Call = (_ => _ => routes.CheckYourAnswersController.onPageLoad())

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Future[Call] = {
    mode match {
      case NormalMode => normalRoutes(page)(userAnswers)
      case CheckMode => Future.successful(checkRouteMap(page)(userAnswers))
    }
  }

  override val errorPage: Page => Call = {
    case individualsPages.DateOfBirthRegPage => individualRoutes.UpdateDobKickOutController.onPageLoad()
    case _ => routes.JourneyRecoveryController.onPageLoad()
  }
}
