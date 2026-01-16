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
import play.api.mvc.{MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsResult.{GrsFailure, GrsSuccess}
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsResult
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository

import scala.concurrent.{ExecutionContext, Future}

abstract class AbstractGrsController( val controllerComponents: MessagesControllerComponents,
                                      registrationDataRepository: RegistrationDataRepository)
                                    ( implicit ec: ExecutionContext) extends Logging with FrontendBaseController with I18nSupport {
  
  def processResponse(userId: String, result: GrsResult): Future[Result] =
    processSuccess(userId).orElse(processFailure)(result)
  
  private def processSuccess(userId: String): PartialFunction[GrsResult, Future[Result]] = {
    case GrsSuccess(utr, safe) =>
      logger.info(s"GRS journey successful")
      for {
        _ <- registrationDataRepository.setCtUtr(userId)(utr)
        _ <- registrationDataRepository.setSafeId(userId)(safe)
    } yield {
      Redirect(orgRoutes.AddressController.onPageLoad().url)
    }
  }
  
  private def processFailure: PartialFunction[GrsResult, Future[Result]] = {
    case GrsFailure(msg) => 
      logger.info(s"GRS registration failed: $msg")
      Future.successful(Redirect(orgRoutes.PartnershipKickOutController.onPageLoad().url))
  }

}
