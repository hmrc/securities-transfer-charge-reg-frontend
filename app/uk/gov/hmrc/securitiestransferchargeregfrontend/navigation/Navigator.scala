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

package uk.gov.hmrc.securitiestransferchargeregfrontend.navigation

import play.api.libs.json.Reads
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargeregfrontend.queries.Gettable
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

trait Navigator:
  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Future[Call]
  val errorPage: Page => Call

abstract class AbstractNavigator(sessionRepository: SessionRepository)(implicit ec: ExecutionContext) extends Navigator:

  protected val defaultPage: Future[Call] = Future.successful(routes.JourneyRecoveryController.onPageLoad())
  
  protected[navigation] def goTo(success: Call): Future[Call] =
    Future.successful(success)

  protected[navigation] def dataRequired[A: Reads](page: Page & Gettable[A], userAnswers: UserAnswers, success: Call): Future[Call] =
    dataDependent(page, userAnswers)(_ => success)

  protected[navigation] def dataDependent[A: Reads](page: Page & Gettable[A], userAnswers: UserAnswers)(f: A => Call): Future[Call] =
    userAnswers.get(page) match {
      case Some(value) =>
        sessionRepository
          .set(userAnswers)
          .collect { case true => f(value) }
      case None => defaultPage
    }
