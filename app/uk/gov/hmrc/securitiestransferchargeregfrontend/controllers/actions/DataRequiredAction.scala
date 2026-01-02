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

/*
* Copyright 2025 HM Revenue & Customs
 *
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
 *
* http://www.apache.org/licenses/LICENSE-2.0
 *
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
 */

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions

import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.*
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

def requestOrRedirect[R](maybeUserAnswers: Option[UserAnswers])(onSuccess: UserAnswers => R): Future[Either[Result, R]] =
  Future.successful(
    maybeUserAnswers
      .map(answers => Right(onSuccess(answers)))
      .getOrElse(Left(Redirect(routes.JourneyRecoveryController.onPageLoad())))
  )

trait DataRequiredAction extends ActionRefiner[OptionalDataRequest, DataRequest]

class DataRequiredActionImpl @Inject()(implicit val executionContext: ExecutionContext) extends DataRequiredAction:

  override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] =
    requestOrRedirect(request.userAnswers) { answers =>
      DataRequest(request.request, request.userId, answers)
    }

trait ValidIndividualDataRequiredAction extends ActionRefiner[ValidIndividualOptionalDataRequest, ValidIndividualDataRequest]

class ValidIndividualDataRequiredActionImpl @Inject()(implicit val executionContext: ExecutionContext) extends ValidIndividualDataRequiredAction:

  override protected def refine[A](request: ValidIndividualOptionalDataRequest[A]): Future[Either[Result, ValidIndividualDataRequest[A]]] =
    requestOrRedirect(request.userAnswers) { answers =>
      ValidIndividualDataRequest(request.request, answers)
    }

trait ValidOrgDataRequiredAction extends ActionRefiner[ValidOrgOptionalDataRequest, ValidOrgDataRequest]

class ValidOrgDataRequiredActionImpl @Inject()(implicit val executionContext: ExecutionContext) extends ValidOrgDataRequiredAction:

  override protected def refine[A](request: ValidOrgOptionalDataRequest[A]): Future[Either[Result, ValidOrgDataRequest[A]]] =
    requestOrRedirect(request.userAnswers) { answers =>
      ValidOrgDataRequest(request.request, answers)
    }