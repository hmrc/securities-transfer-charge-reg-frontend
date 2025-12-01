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

import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.*
import play.api.test.Helpers.stubBodyParser
import uk.gov.hmrc.securitiestransferchargeregfrontend.base.SpecBase
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.IdentifierRequest

import scala.concurrent.{ExecutionContext, Future}

class FakeAuthenticatedIdentifierAction extends IdentifierAction with SpecBase with ScalaFutures {

  override val parser: BodyParser[AnyContent] = stubBodyParser(AnyContentAsEmpty)
  override protected val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

  override def invokeBlock[A](
                               request: Request[A],
                               block: IdentifierRequest[A] => Future[Result]
                             ): Future[Result] = {

    block(IdentifierRequest(request, "id", Some(fakeUserDetails)))
  }
}


