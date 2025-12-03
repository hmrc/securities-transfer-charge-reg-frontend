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


import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.DateOfBirthRegPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.DateOfBirthRegFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.DateOfBirthRegView
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.Mode
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject


class DateOfBirthRegController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: DateOfBirthRegFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: DateOfBirthRegView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { ////////// ADD THE andThen requireData
    implicit request =>
      val userAnswers = request.userAnswers.getOrElse(new UserAnswers(request.userId)) ///////////// Created a dumm data

      val form = formProvider()

      val preparedForm =
        request.userAnswers.flatMap(_.get(DateOfBirthRegPage)) match {
          case None => form
          case Some(value) => form.fill(value)
        }
      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { ////////// ADD THE andThen requireData
    implicit request =>
      val userAnswers = request.userAnswers.getOrElse(new UserAnswers(request.userId)) /////// REMOVE THIS LINE

      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(userAnswers.set(DateOfBirthRegPage, value)) ////// ADD request BEFORE userAnswers
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(DateOfBirthRegPage, mode, updatedAnswers))
      )
  }
}
