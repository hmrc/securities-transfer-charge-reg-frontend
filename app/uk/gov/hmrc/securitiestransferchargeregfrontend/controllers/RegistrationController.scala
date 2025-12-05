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

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers

import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.*
import uk.gov.hmrc.auth.core.retrieve.ItmpName
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.Auth
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcAuthRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

/*
 * This controller handles the registration routing logic based on the user's Affinity Group.
 */
@Singleton
class RegistrationController @Inject()(
                                        mcc: MessagesControllerComponents,
                                        redirects: Redirects,
                                        auth: Auth,
                                      ) extends  FrontendController(mcc) with Logging {

  import redirects.*
  private[controllers] val retrievals = Retrievals.affinityGroup and Retrievals.confidenceLevel and Retrievals.nino and Retrievals.itmpName

  private[controllers] val checkConfidence: ConfidenceLevel => Boolean = _ >= ConfidenceLevel.L250
  private[controllers] val checkName: ItmpName => Boolean = n => n.givenName.isDefined && n.familyName.isDefined


  /*
   * Individuals require a confidence level of 250 or above with both a name and NINO to register directly.
   * Otherwise, they are redirected to the IV uplift process.
   * Organisation users are redirected to the organisation registration page which collects the type of company they are
   * and then sends them on the appropriate GRS journey.
   * Agents are redirected to the Agent Services Account (ASA) home page as they do not need to register.
   */
  val routingLogic: Action[AnyContent] = auth.authorisedAndNotEnrolled.async { implicit request =>
    request.affinityGroup match {
      case Individual   => routeIndividuals(request.confidenceLevel, request.maybeNino, request.maybeName)
      case Organisation => redirectToRegisterOrganisationF
      case Agent        => redirectToAsaF
    }
  }

  private[controllers] def routeIndividuals(
    confidenceLevel: ConfidenceLevel,
    maybeNino: Option[String],
    maybeName: Option[ItmpName]
  )(implicit request: StcAuthRequest[AnyContent]): Future[Result] = {
    if (checkConfidence(confidenceLevel) && maybeNino.isDefined && maybeName.exists(checkName)) {
      redirectToRegisterIndividualF
    } else {
      redirectToIVUpliftF
    }
  }
}
