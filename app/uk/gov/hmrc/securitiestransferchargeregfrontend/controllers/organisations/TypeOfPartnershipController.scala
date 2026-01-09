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

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations.TypeOfPartnershipFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.TypeOfPartnership
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.TypeOfPartnershipPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.TypeOfPartnershipView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class TypeOfPartnershipController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             sessionRepository: SessionRepository,
                                             @Named("organisations") navigator: Navigator,                                             auth: OrgAuth,
                                             formProvider: TypeOfPartnershipFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: TypeOfPartnershipView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import auth.*

  val form: Form[TypeOfPartnership] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (validOrg andThen getData) {
    implicit request =>

      val preparedForm = request.userAnswers
        .flatMap(_.get(TypeOfPartnershipPage))
        .fold(form)(form.fill)

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(TypeOfPartnershipPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(TypeOfPartnershipPage, mode, updatedAnswers))
      )
  }
}
