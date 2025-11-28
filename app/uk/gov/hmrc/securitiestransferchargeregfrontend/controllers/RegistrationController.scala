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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{ItmpName, ~}
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.StcAuthAction

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/*
 * This controller handles the registration routing logic based on the user's Affinity Group and enrolment status.
 */
@Singleton
class RegistrationController @Inject()(
                                        mcc: MessagesControllerComponents,
                                        appConfig: FrontendAppConfig,
                                        redirects: Redirects,
                                        val authConnector: AuthConnector,
                                        auth: StcAuthAction,
                                      ) (implicit ec: ExecutionContext) extends  FrontendController(mcc) with AuthorisedFunctions with Logging {

  import redirects.*
  private[controllers] val retrievals = Retrievals.affinityGroup and Retrievals.confidenceLevel and Retrievals.nino and Retrievals.itmpName

  private[controllers] val enrolledForSTC: Enrolments => Boolean = _.getEnrolment(appConfig.stcEnrolmentKey).isDefined
  private[controllers] val checkConfidence: ConfidenceLevel => Boolean = _ >= ConfidenceLevel.L250
  private[controllers] val checkName: Option[ItmpName] => Boolean = maybeName => maybeName.exists(n => n.givenName.isDefined && n.familyName.isDefined)

  /*
   * Individuals require a confidence level of 250 or above and a name and NINO to register directly.
   * otherwise, they are redirected to the IV uplift process.
   * Organisations are redirected to the organisation registration page which collects the type of company they are
   * and then sends them on the appropriate GRS journey.
   * Agents are redirected to the Agent Services Account (ASA) home page as they do not need to register.
   */
  val routingLogic: Action[AnyContent] = auth.authorise.async { implicit request =>
    authorised().retrieve(retrievals) {
      case Some(Individual) ~ cl ~ Some(nino) ~ name if checkConfidence(cl) && checkName(name)
                                            => Future.successful(redirectToRegisterIndividual)
      case Some(Individual) ~ _ ~ _ ~ _     => Future.successful(redirectToIVUplift)
      case Some(Organisation) ~ _ ~ _  ~ _  => Future.successful(redirectToRegisterOrganisation)
      case Some(Agent) ~ _ ~ _ ~ _          => Future.successful(redirectToASA)
      case _                                => Future.failed(new UnauthorizedException("Could not retrieve the user's Affinity Group"))
    } recover {
      case _: AuthorisationException                                => redirectToLogin
    }
  }
}