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
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.AlfAddressConnector
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.AlfConfirmedAddress

import scala.concurrent.Future


abstract class AbstractAddressController(alf: AlfAddressConnector) extends FrontendBaseController with I18nSupport with Logging {
  
  /*
   * Creates an address journey and redirects to the to it.
   * If the journey fails to initialise, the user is sent to an error page.
   */
  def pageLoad(configFileLocation: String, returnUrl: String)(implicit hc: HeaderCarrier): Future[Result] = {
      alf.initAlfJourneyRequest(configFileLocation, returnUrl)
  }

  /*
   * Retrieves the outcome of the journey and stores the address in UserAnswers if
   * it was successful. If retrieval fails the user is sent to an error page.
   */
  def alfReturn(addressId: String)(implicit hc: HeaderCarrier): Future[AlfConfirmedAddress] = {
    logger.info("Address lookup frontend has returned control to STC service")
    alf.alfRetrieveAddress(addressId)
  }
  
}
