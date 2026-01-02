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

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions

import play.api.mvc.ActionTransformer
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

val getUserAnswers: SessionRepository => String => Future[Option[UserAnswers]] =
  sessionRepository => userId => sessionRepository.get(userId)

trait DataRetrievalAction extends ActionTransformer[StcAuthRequest, OptionalDataRequest]

class DataRetrievalActionImpl @Inject()(sessionRepository: SessionRepository)
                                       (implicit val executionContext: ExecutionContext) extends DataRetrievalAction:

  override protected def transform[A](request: StcAuthRequest[A]): Future[OptionalDataRequest[A]] =
    getUserAnswers(sessionRepository)(request.userId).map { answers =>
      OptionalDataRequest(request, request.userId, userAnswers = answers)
    }

trait ValidIndividualDataRetrievalAction extends ActionTransformer[StcValidIndividualRequest, ValidIndividualOptionalDataRequest]

class ValidIndividualDataRetrievalActionImpl @Inject()(sessionRepository: SessionRepository)
                                       (implicit val executionContext: ExecutionContext) extends ValidIndividualDataRetrievalAction:

  override protected def transform[A](request: StcValidIndividualRequest[A]): Future[ValidIndividualOptionalDataRequest[A]] =
    getUserAnswers(sessionRepository)(request.userId).map { answers =>
      ValidIndividualOptionalDataRequest(request, answers)
    }
    
trait ValidOrgDataRetrievalAction extends ActionTransformer[StcValidOrgRequest, ValidOrgOptionalDataRequest]

class ValidOrgDataRetrievalActionImpl @Inject()(sessionRepository: SessionRepository)
                                       (implicit val executionContext: ExecutionContext) extends ValidOrgDataRetrievalAction:

  override protected def transform[A](request: StcValidOrgRequest[A]): Future[ValidOrgOptionalDataRequest[A]] =
    getUserAnswers(sessionRepository)(request.userId).map { answers =>
      ValidOrgOptionalDataRequest(request, answers)
    }

