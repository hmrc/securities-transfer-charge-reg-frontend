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

package uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests

import play.api.mvc.WrappedRequest
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers

case class OptionalDataRequest[A] (request: StcAuthRequest[A], userId: String, userAnswers: Option[UserAnswers]) extends WrappedRequest[A](request)

case class DataRequest[A] (request: StcAuthRequest[A], userId: String, userAnswers: UserAnswers) extends WrappedRequest[A](request)

case class ValidIndividualOptionalDataRequest[A](request: StcValidIndividualRequest[A], userAnswers: Option[UserAnswers]) extends WrappedRequest[A](request)

case class ValidIndividualDataRequest[A] (request: StcValidIndividualRequest[A], userAnswers: UserAnswers) extends WrappedRequest[A](request)

case class ValidOrgOptionalDataRequest[A](request: StcValidOrgRequest[A], userAnswers: Option[UserAnswers]) extends WrappedRequest[A](request)

case class ValidOrgDataRequest[A] (request: StcValidOrgRequest[A], userAnswers: UserAnswers) extends WrappedRequest[A](request)