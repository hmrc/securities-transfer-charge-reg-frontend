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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.RegForSecuritiesTransferChargePage
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.RegForSecuritiesTransferChargeView

import javax.inject.{Inject, Named}

class RegForSecuritiesTransferChargeController @Inject()(
                                                          auth: OrgAuth,
                                                          @Named("organisations") navigator: Navigator,
                                                          val controllerComponents: MessagesControllerComponents,
                                                          view: RegForSecuritiesTransferChargeView
                                     ) extends FrontendBaseController with I18nSupport {

  import auth.*

  def onPageLoad: Action[AnyContent] = validOrg {
    implicit request =>
      Ok(view())
  }

  def onSubmit(): Action[AnyContent] = validOrg {
    implicit request =>
      Redirect(navigator.nextPage(RegForSecuritiesTransferChargePage, NormalMode, UserAnswers("")))
  }
}
