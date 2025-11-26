/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package repositories

import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

import scala.concurrent.Future

class FakeSessionRepository extends SessionRepository {

  override def get(id: String): Future[Option[UserAnswers]] = Future.successful(None)

  override def set(answers: UserAnswers): Future[Boolean] = Future.successful(true)

  override def clear(id: String): Future[Boolean] = Future.successful(true)

  override def keepAlive(id: String): Future[Boolean] = Future.successful(true)
}