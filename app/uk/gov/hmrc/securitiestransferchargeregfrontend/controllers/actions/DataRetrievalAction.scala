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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.{IdentifierRequest, OptionalDataRequest}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

class DataRetrievalActionImpl @Inject()(
                                         sessionRepository: SessionRepository
                                       )(implicit ec: ExecutionContext)
  extends DataRetrievalAction {

  override protected def transform[A](
                                       request: IdentifierRequest[A]
                                     ): Future[OptionalDataRequest[A]] = {

    sessionRepository.get(request.userId).map { answers =>
      OptionalDataRequest(
        request      = request.request,
        userId       = request.userId,
        userDetails  = request.userDetails,
        userAnswers  = answers
      )
    }
  }

  override protected def executionContext: ExecutionContext = ec
}


trait DataRetrievalAction extends ActionTransformer[IdentifierRequest, OptionalDataRequest]
