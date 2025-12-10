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

import play.api.data.Form
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*

import javax.inject.Inject
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.WhatsYourContactNumberFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.WhatsYourContactNumberPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.WhatsYourContactNumberView
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.Auth

import scala.concurrent.{ExecutionContext, Future}

class WhatsYourContactNumberController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        auth: Auth,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: WhatsYourContactNumberFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: WhatsYourContactNumberView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (auth.authorisedIndividualAndNotEnrolled andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(WhatsYourContactNumberPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (auth.authorisedIndividualAndNotEnrolled andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(WhatsYourContactNumberPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(WhatsYourContactNumberPage, mode, updatedAnswers))
      )
  }
}
