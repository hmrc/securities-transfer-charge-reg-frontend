/*
 * Copyright 2022 HM Revenue & Customs
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

package repositories

import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

import scala.concurrent.Future

class FakeSessionRepository extends SessionRepository:

  override def get(id: String): Future[Option[UserAnswers]] = Future.successful(None)

  override def set(answers: UserAnswers): Future[Boolean] = Future.successful(true)

  override def clear(id: String): Future[Boolean] = Future.successful(true)

  override def keepAlive(id: String): Future[Boolean] = Future.successful(true)

  override def updateAndStore(key: String, updateFn: UserAnswers => UserAnswers): Future[UserAnswers] =
    Future.successful(updateFn(UserAnswers(key)))
