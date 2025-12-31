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

package controllers.actions

import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.{DataRetrievalAction, ValidIndividualDataRetrievalAction}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.{OptionalDataRequest, StcAuthRequest, StcValidIndividualRequest, ValidIndividualOptionalDataRequest}

import scala.concurrent.{ExecutionContext, Future}

class FakeDataRetrievalAction(
                               dataToReturn: Option[UserAnswers]
                             ) extends DataRetrievalAction {

  override protected def transform[A](
                                       request: StcAuthRequest[A]
                                     ): Future[OptionalDataRequest[A]] =
    Future.successful(
      OptionalDataRequest(
        request     = request,
        userId      = request.userId,
        userAnswers = dataToReturn
      )
    )

  override protected implicit val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}

class FakeValidIndividualDataRetrievalAction(
                               dataToReturn: Option[UserAnswers]
                             ) extends ValidIndividualDataRetrievalAction {

  override protected def transform[A](
                                       request: StcValidIndividualRequest[A]
                                     ): Future[ValidIndividualOptionalDataRequest[A]] =
    Future.successful(
      ValidIndividualOptionalDataRequest(request, dataToReturn)
    )
    

  override protected implicit val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}

