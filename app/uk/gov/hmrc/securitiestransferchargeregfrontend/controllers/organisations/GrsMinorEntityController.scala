/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsMinorEntityConnector
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.OrgAuth
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class GrsMinorEntityController @Inject() (controllerComponents: MessagesControllerComponents,
                                          connector: GrsMinorEntityConnector,
                                          auth: OrgAuth,
                                          @Named("organisations") navigator: Navigator,
                                          dataRepository: RegistrationDataRepository)
                                         (implicit ec: ExecutionContext) extends BaseGrsController(controllerComponents, dataRepository, navigator):

  import auth.*
  
  def trustJourney: Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      connector.initTrustJourney
  }

  def unincorporatedAssociationJourney: Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      connector.initUnincorporatedAssociationJourney
  }

  def returnFromJourney(journeyId: String): Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>
      connector.retrieveGrsResults(journeyId).flatMap { result =>
        super.processResponse(request.userAnswers, result)
      }
  }
