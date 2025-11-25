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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AffinityGroup.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel, Enrolments}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.EnrolmentCheck

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationController @Inject()(
                                        mcc: MessagesControllerComponents,
                                        redirects: Redirects,
                                        val authConnector: AuthConnector,
                                        enrolmentCheck: EnrolmentCheck
                                      ) (implicit ec: ExecutionContext) extends  FrontendController(mcc) with AuthorisedFunctions {

  import redirects.*
  private[controllers] val predicates = ConfidenceLevel.L250
  private[controllers] val retrievals = Retrievals.affinityGroup and Retrievals.nino and Retrievals.itmpName

  private[controllers] val enrolledForSTC: Enrolments => Boolean = _.getEnrolment("HMRC-STC-ORG").isDefined

  val routingLogic: Action[AnyContent] = enrolmentCheck.async { implicit request =>
    authorised(predicates).retrieve(retrievals) {
      case Some(Individual) ~ Some(nino) ~ Some(name) => Future.successful(redirectToRegisterIndividual)
      case Some(Individual) ~ _ ~ _                   => Future.successful(redirectToIVUplift)
      case Some(Organisation) ~ _ ~ _                 => Future.successful(redirectToRegisterOrganisation)
      case Some(Agent) ~ _ ~ _                        => Future.successful(redirectToASA)
      case _                                          => Future.failed(new Exception("Could not retrieve the user's Affinity Group"))
    } recover {
      case _ => redirectToLogin
    }
  }
}