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

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.AlfAddressConnector
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.AbstractAddressController
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.OrgAuth
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.http.HeaderCarrier

class AddressController @Inject() (val controllerComponents: MessagesControllerComponents,
                                   alf: AlfAddressConnector,
                                   sessionRepository: SessionRepository,
                                   auth: OrgAuth,
                                   @Named("organisations") val navigator: Navigator,
                                   config: FrontendAppConfig)
                                  (implicit ec: ExecutionContext) extends AbstractAddressController(alf, sessionRepository) {

  import auth.*

  def onPageLoad: Action[AnyContent] = validOrg.async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      super.pageLoad(config.organisationsAlfConfigFileLocation, config.alfOrgContinueUrl)
  }

  def onReturn(addressId: String): Action[AnyContent] = (validOrg andThen getData).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val userId = request.request.userId
      super.alfReturn(addressId, userId)
  }
}
