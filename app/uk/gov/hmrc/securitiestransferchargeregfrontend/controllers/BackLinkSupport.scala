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

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers

import play.api.mvc.{Call, Result}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

trait BackLinkSupport {

  protected def withBackLink(
                              navigator: Navigator,
                              page: Page,
                              mode: Mode,
                              userAnswers: UserAnswers
                            )(block: Call => Result)
                            (implicit ec: ExecutionContext): Future[Result] = {

    navigator.previousPage(page, mode, userAnswers)
      .map(block)
  }

  protected def withBackLink(
                              navigator: Navigator,
                              page: Page,
                              mode: Mode,
                              userAnswersOpt: Option[UserAnswers]
                            )(block: Call => Result)
                            (implicit ec: ExecutionContext): Future[Result] = {

    userAnswersOpt match {
      case Some(ua) =>
        navigator.previousPage(page, mode, ua).map(block)

      case None =>
        Future.successful(block(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
