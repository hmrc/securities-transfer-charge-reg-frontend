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

import com.google.inject.ImplementedBy
import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.auth.core.retrieve.ItmpName
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcAuthRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/*
  An ActionFilter that is used to protect endpoints in this service.
  It protects the invariant that users are both authenticated and NOT already enrolled.
  If the user is already enrolled, they are redirected to the service proper.
  If the user is not authenticated, they are redirected to login.
  Otherwise they are allowed to proceed.
*/

@ImplementedBy(classOf[IndividualCheckImpl])
trait IndividualCheck extends ActionFilter[StcAuthRequest]

class IndividualCheckImpl @Inject()(val parser: BodyParsers.Default,
                                   redirects: Redirects,
                                   val authConnector: AuthConnector )(implicit ec: ExecutionContext)
  extends IndividualCheck with Logging {

  import redirects.*

  override protected def executionContext: ExecutionContext = ec

  private[controllers] val checkConfidence: ConfidenceLevel => Boolean = _ >= ConfidenceLevel.L250
  private[controllers] val checkName: ItmpName => Boolean = n => n.givenName.isDefined && n.familyName.isDefined

  override protected def filter[A](request: StcAuthRequest[A]): Future[Option[Result]] = {
    if (checkConfidence(request.confidenceLevel) &&
        request.maybeName.exists(checkName) &&
        request.maybeNino.isDefined) {
      Future.successful(None)
    } else {
      redirectToRegisterF.map(Option.apply)
    }
  }
}
