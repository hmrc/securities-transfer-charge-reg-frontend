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

import connectors.AlfAddressConnector
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidOrgOptionalDataRequest
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{AlfConfirmedAddress, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.AddressPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class AddressController @Inject()(auth: OrgAuth,
                                  navigator: Navigator,
                                  val controllerComponents: MessagesControllerComponents,
                                  alf: AlfAddressConnector,
                                  sessionRepository: SessionRepository
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  import auth.*

  /*
   * Creates an address journey and redirects to the to it.
   * If the journey fails to initialise, the user is sent to an error page.
   */
  def onPageLoad: Action[AnyContent] = validOrg.async {
    implicit request =>
      alf.initAlfJourneyRequest()
  }

  /*
   * Retrieves the outcome of the journey and stores the address in UserAnswers if
   * it was successful. If retrieval fails the user is sent to an error page.
   */
  def onReturn(id: String): Action[AnyContent] = (auth.validOrg andThen getData).async {
    implicit request =>
      logger.info("Address lookup frontend has returned control to STC service")
      for {
        address <- alf.alfRetrieveAddress(id)
        answers <- updateUserAnswers(request)(address)
      } yield {
        Redirect(navigator.nextPage(AddressPage(), NormalMode, answers))
      }
  }

  private type AddressHandler = PartialFunction[AlfConfirmedAddress, Future[UserAnswers]]

  private def updateUserAnswers[A](implicit request: ValidOrgOptionalDataRequest[A]): AddressHandler =
    address =>
      logger.info("ALF returned address successfully")
      sessionRepository.updateAndStore(
        request.request.userId,
        _.set(AddressPage[AlfConfirmedAddress](), address).get
      )

}