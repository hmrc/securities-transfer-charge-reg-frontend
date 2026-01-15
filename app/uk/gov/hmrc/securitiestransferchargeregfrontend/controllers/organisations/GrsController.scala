/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations

import routes as orgRoutes

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsResult.GrsSuccess
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.{GrsResult, IncorporatedEntityGrsConnector}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.OrgAuth
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.SelectBusinessType.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.SelectBusinessTypePage
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class GrsController @Inject() (val controllerComponents: MessagesControllerComponents,
                               ieConnector: IncorporatedEntityGrsConnector,
                               auth: OrgAuth,
                               @Named("organisations") val navigator: Navigator,
                               registrationDataRepository: RegistrationDataRepository)
                              ( implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  import auth.*
  
  def onPageLoad: Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      request.userAnswers.get(SelectBusinessTypePage) match {
        case None => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
        case Some(LimitedCompany) => ieConnector.initLimitedCompanyJourney
        case Some(RegisteredSociety) => ieConnector.initRegisteredSocietyJourney
        case _ => throw new Exception("GRS journey failed")
      }
  }
  
  def onReturn(journeyId: String): Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>
      val userId = request.request.userId
      ieConnector.retrieveGrsResults(journeyId)
        .flatMap(processSuccess(userId).orElse(processFailure))
  }
  
  private def processSuccess(userId: String): PartialFunction[GrsResult, Future[Result]] =  {
    case GrsSuccess(utr, safe) =>
      logger.info(s"GRS journey successful for userId: $userId with UTR: $utr and SafeId: $safe")
      for {
      _ <- registrationDataRepository.setCtUtr(userId)(utr)
      _ <- registrationDataRepository.setSafeId(userId)(safe)
    } yield {
      Redirect(orgRoutes.AddressController.onPageLoad().url)
    }
  }
  
  private def processFailure: PartialFunction[GrsResult, Future[Result]] = {
    case _ => Future.successful(
      Redirect(orgRoutes.PartnershipKickOutController.onPageLoad().url)
    )
  }

}
