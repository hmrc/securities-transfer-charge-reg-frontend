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

import play.api.mvc.*
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.IdentifierAction
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserDetails
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.IdentifierRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FakeIdentifierAction @Inject()(
                                      bodyParsers: PlayBodyParsers
                                    )(implicit ec: ExecutionContext)
  extends IdentifierAction {

  private var currentUserDetails: UserDetails =
    UserDetails(
      firstName       = Some("Test"),
      lastName        = Some("User"),
      affinityGroup   = AffinityGroup.Individual,
      confidenceLevel = ConfidenceLevel.L200,
      nino            = Some("AA123456A")
    )

  def withUserDetails(details: UserDetails): FakeIdentifierAction = {
    this.currentUserDetails = details
    this
  }

  override def invokeBlock[A](
                               request: Request[A],
                               block: IdentifierRequest[A] => Future[Result]
                             ) =
    block(
      IdentifierRequest(
        request,
        "id",
        currentUserDetails
      )
    )

  override def parser = bodyParsers.default

  override protected def executionContext: ExecutionContext = ec
}


