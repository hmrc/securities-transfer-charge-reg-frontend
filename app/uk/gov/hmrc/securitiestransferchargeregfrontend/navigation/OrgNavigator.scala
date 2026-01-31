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
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.SelectBusinessType.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.TypeOfPartnership.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.{organisations as organisationsPages, *}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrgNavigator @Inject()(sessionRepository: SessionRepository)
                            (implicit ec: ExecutionContext) extends AbstractNavigator(sessionRepository) {

  private val normalRoutes: Page => UserAnswers => Future[Call] = {

    case organisationsPages.RegForSecuritiesTransferChargePage =>
      _ => goTo(orgRoutes.UkOrNotController.onPageLoad(NormalMode))

    case organisationsPages.UkOrNotPage =>
      userAnswers => dataDependent(organisationsPages.UkOrNotPage, userAnswers) { isUk =>
        if (isUk) orgRoutes.SelectBusinessTypeController.onPageLoad(NormalMode)
        else orgRoutes.UkOrNotKickOutController.onPageLoad()
      }

    case organisationsPages.SelectBusinessTypePage =>
      userAnswers => dataDependent(organisationsPages.SelectBusinessTypePage, userAnswers) {
        case LimitedCompany             => orgRoutes.GrsIncorporatedEntityController.limitedCompanyJourney
        case Partnership                => orgRoutes.TypeOfPartnershipController.onPageLoad(NormalMode)
        case SoleTrader                 => orgRoutes.PartnershipKickOutController.onPageLoad()
        case Trust                      => orgRoutes.GrsMinorEntityController.trustJourney
        case RegisteredSociety          => orgRoutes.GrsIncorporatedEntityController.registeredSocietyJourney
        case UnincorporatedAssociation  => orgRoutes.GrsMinorEntityController.unincorporatedAssociationJourney
      }

    case organisationsPages.TypeOfPartnershipPage =>
      userAnswers => dataDependent(organisationsPages.TypeOfPartnershipPage, userAnswers) {
        case ScottishLimitedPartnership   => orgRoutes.GrsPartnershipController.scottishLimitedPartnershipJourney
        case LimitedPartnership           => orgRoutes.GrsPartnershipController.limitedPartnershipJourney
        case LimitedLiabilityPartnership  => orgRoutes.GrsPartnershipController.limitedLiabilityPartnershipJourney
        case _                            => orgRoutes.PartnershipKickOutController.onPageLoad()
      }
      
    case organisationsPages.GrsPage =>
      _ => goTo(orgRoutes.AddressController.onPageLoad())

    case organisationsPages.OrgAddressPage =>
        userAnswers => dataRequired(organisationsPages.OrgAddressPage, userAnswers, orgRoutes.ContactEmailAddressController.onPageLoad(NormalMode))
      
    case organisationsPages.ContactEmailAddressPage =>
      userAnswers => dataRequired(organisationsPages.ContactEmailAddressPage, userAnswers, orgRoutes.ContactNumberController.onPageLoad(NormalMode))

    case organisationsPages.ContactNumberPage =>
      userAnswers => dataRequired(organisationsPages.ContactNumberPage, userAnswers, orgRoutes.RegistrationCompleteController.onPageLoad())

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
    // TODO: This is not the right kick out page.
    case organisationsPages.GrsPage => orgRoutes.PartnershipKickOutController.onPageLoad()
    case organisationsPages.ContactNumberPage => routes.JourneyRecoveryController.onPageLoad()
    case organisationsPages.OrgAddressPage => ???
    case _ => routes.JourneyRecoveryController.onPageLoad()
  }
}
