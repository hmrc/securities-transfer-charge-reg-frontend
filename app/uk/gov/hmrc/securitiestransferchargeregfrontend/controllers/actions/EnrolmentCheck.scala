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
import uk.gov.hmrc.auth.core.retrieve.v2.*
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolments, InsufficientConfidenceLevel}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.RegistrationClient
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/*
  An ActionBuilder that checks whether the user is enrolled for the Securities Transfer Charge service
  and has a current subscription. If both conditions are met, the user is redirected to the service.
  If not enrolled or no current subscription, the user is redirected to the registration page.
  If not logged in, they are redirected to the login page.
  Note: confidence level is not checked here as it only applies to Individuals and is handled in the RegistrationController.

  This action builder should be used by all controllers to protect their endpoints.
*/

@ImplementedBy(classOf[EnrolmentCheckImpl])
trait EnrolmentCheck extends ActionBuilder[Request, AnyContent]

class EnrolmentCheckImpl @Inject()(val parser: BodyParsers.Default,
                               appConfig: FrontendAppConfig,
                               redirects: Redirects,
                               registrationClient: RegistrationClient,
                               val authConnector: AuthConnector )(implicit ec: ExecutionContext)
  extends EnrolmentCheck with AuthorisedFunctions with Logging {

  import redirects.*

  private[controllers] val retrievals = Retrievals.authorisedEnrolments
  private[controllers] val enrolledForSTC: Enrolments => Boolean = _.getEnrolment(appConfig.stcEnrolmentKey).isDefined

  private[controllers] def hasCurrentSubscription = registrationClient.hasCurrentSubscription

  override protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    // Provide an implicit HeaderCarrier for the authorised call
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(retrievals) {
      case enrolments: Enrolments if enrolledForSTC(enrolments) && hasCurrentSubscription => Future.successful(redirectToService)
      case _ => Future.successful(redirectToRegister)
    } recover {
      case _                              => redirectToLogin
    }
  }
}