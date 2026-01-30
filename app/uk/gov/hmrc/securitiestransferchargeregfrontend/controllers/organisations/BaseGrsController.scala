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

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsResult.{GrsFailure, GrsSuccess}
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsResult
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.GrsPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository

import scala.concurrent.{ExecutionContext, Future}

class BaseGrsController(val controllerComponents: MessagesControllerComponents,
                        registrationDataRepository: RegistrationDataRepository,
                        navigator: Navigator)
                       (implicit ec: ExecutionContext) extends Logging with FrontendBaseController with I18nSupport:
  
  def processResponse(userAnswers: UserAnswers, result: GrsResult): Future[Result] =
    processSuccess(userAnswers).orElse(processFailure)(result)
  
  private def processSuccess(userAnswers: UserAnswers): PartialFunction[GrsResult, Future[Result]] = {
    case GrsSuccess(utr, safe) =>
      logger.info("GRS journey succeeded - processing results")
      (
        for {
          _         <- registrationDataRepository.setCtUtr(userAnswers.id)(utr)
          _         <- registrationDataRepository.setSafeId(userAnswers.id)(safe)
          nextPage  <- navigator.nextPage(GrsPage, NormalMode, userAnswers)
          _         =  logger.info("GRS data processed - redirecting to next page.")
        } yield Redirect(nextPage)
      ).recover {
        case _ => Redirect(navigator.errorPage(GrsPage))
      }
  }
  
  private def processFailure: PartialFunction[GrsResult, Future[Result]] = {
    case GrsFailure(reason) =>
      logger.warn(s"GRS journey failed: $reason")
      Future.successful(Redirect(navigator.errorPage(GrsPage)))
  }
