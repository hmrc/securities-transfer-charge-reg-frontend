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
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.RegistrationClient
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionStatus.SubscriptionActive
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcAuthRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/*
  An ActionFilter that is used to protect endpoints in this service.
  It protects the invariant that users are both authenticated and NOT already enrolled.
  If the user is already enrolled, they are redirected to the service proper.
  If the user is not authenticated, they are redirected to log in.
  Otherwise, they are allowed to proceed.
*/

@ImplementedBy(classOf[EnrolmentCheckImpl])
trait EnrolmentCheck extends ActionFilter[StcAuthRequest]

class EnrolmentCheckImpl @Inject()(val parser: BodyParsers.Default,
                                   appConfig: FrontendAppConfig,
                                   redirects: Redirects,
                                   registrationClient: RegistrationClient)
                                  (implicit ec: ExecutionContext)
  extends EnrolmentCheck with Logging {

  import redirects.*

  val enrolledForSTC: Enrolments => Boolean = es => {
    val stcEnrolment = es.getEnrolment(appConfig.stcEnrolmentKey)
    stcEnrolment.exists(_.isActivated)
  }

  // TODO: We need a way to get the safe-id for the current user
  private[controllers] def hasCurrentSubscription: Future[Boolean]
    = registrationClient.hasCurrentSubscription("safe-id").map(_ == Right(SubscriptionActive))

  override protected def executionContext: ExecutionContext = ec

  override protected def filter[A](request: StcAuthRequest[A]): Future[Option[Result]] = {
    hasCurrentSubscription.map { success =>
    if (enrolledForSTC(request.enrolments) && success) {
      Some(redirectToService)
    } else {
      None
    }
  }
  }
}
