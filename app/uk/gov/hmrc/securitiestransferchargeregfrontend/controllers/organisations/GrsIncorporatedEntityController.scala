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

import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsIncorporatedEntityConnector
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.OrgAuth
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GrsIncorporatedEntityController @Inject() (controllerComponents: MessagesControllerComponents,
                                                 connector: GrsIncorporatedEntityConnector,
                                                 auth: OrgAuth,
                                                 dataRepository: RegistrationDataRepository)
                                                (implicit ec: ExecutionContext) extends BaseGrsController(controllerComponents, dataRepository) with Logging:
  
  import auth.*
  
  def limitedCompanyJourney: Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      connector.initLimitedCompanyJourney
  }

  def registeredSocietyJourney: Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      connector.initRegisteredSocietyJourney
  }
  
  def returnFromJourney(journeyId: String): Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>
      logger.info("GRS Incorporated Entity: returned from journey with id " + journeyId)
      connector.retrieveGrsResults(journeyId).flatMap { result =>
        super.processResponse(request.request.userId, result)
      }
  }
  
  
