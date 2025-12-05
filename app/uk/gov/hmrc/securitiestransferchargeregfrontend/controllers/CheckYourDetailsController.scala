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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.CheckYourDetailsFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.CheckYourDetailsPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.CheckYourDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourDetailsController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            sessionRepository: SessionRepository,
                                            navigator: Navigator,
                                            auth: Auth,
                                            getData: DataRetrievalAction,
                                            formProvider: CheckYourDetailsFormProvider,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourDetailsView
                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (auth.authorisedIndividualAndNotEnrolled andThen getData) { implicit request =>
      val preparedForm = request.userAnswers
        .flatMap(_.get(CheckYourDetailsPage))
        .fold(form)(form.fill)

      val firstName = request.request.maybeName.flatMap(_.givenName).get
      val lastName = request.request.maybeName.flatMap(_.familyName).get
      val nino = request.request.maybeNino.get

      Ok(view(preparedForm, firstName, lastName, nino, mode))

    }


  def onSubmit(mode: Mode): Action[AnyContent] =
    (auth.authorisedIndividualAndNotEnrolled andThen getData).async { implicit request =>

      val firstName = request.request.maybeName.flatMap(_.givenName).get
      val lastName = request.request.maybeName.flatMap(_.familyName).get
      val nino = request.request.maybeNino.get


      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, firstName, lastName, nino, mode))
          ),
        value => {
          val updatedAnswers =
            request.userAnswers
              .getOrElse(UserAnswers(request.userId))
              .set(CheckYourDetailsPage, value)
              .get

          sessionRepository.set(updatedAnswers).map { _ =>
            Redirect(navigator.nextPage(CheckYourDetailsPage, mode, updatedAnswers))
          }
        }
      )
    }
}
