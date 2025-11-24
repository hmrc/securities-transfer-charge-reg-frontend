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

import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.auth.core.retrieve.v2.*
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.Redirects

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentCheck @Inject()(val parser: BodyParsers.Default,
                               appConfig: FrontendAppConfig,
                               redirects: Redirects,
                               val authConnector: AuthConnector )(implicit ec: ExecutionContext)
  extends ActionBuilder[Request, AnyContent] with AuthorisedFunctions with Logging {

  import redirects.*
  private[controllers] val retrievals = Retrievals.authorisedEnrolments
  private[controllers] val enrolledForSTC: Enrolments => Boolean = _.getEnrolment(appConfig.stcEnrolmentKey).isDefined

  /*  TODO: We will need to implement a check to see if the user has a current subscription
   *  TODO: this will required finding their subscription in EACD and then asking ETMP what the end date on it is.
   */
  private[controllers] val hasCurrentSubscription: () => Boolean = () => true

  override protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    // Provide an implicit HeaderCarrier for the authorised call
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised().retrieve(retrievals) {
      case enrolments: Enrolments if enrolledForSTC(enrolments) && hasCurrentSubscription() => Future.successful(redirectToService)
      case _ => Future.successful(redirectToRegister)
    } recover {
      case _ => redirectToLogin
    }
  }
}