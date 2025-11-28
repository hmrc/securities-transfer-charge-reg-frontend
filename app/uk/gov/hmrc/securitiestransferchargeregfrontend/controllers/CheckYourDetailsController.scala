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

import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.CheckYourDetailsFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.CheckYourDetailsPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.CheckYourDetailsView
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourDetailsController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         authenticatedIdentifierAction: AuthenticatedIdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: CheckYourDetailsFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: CheckYourDetailsView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen authenticatedIdentifierAction andThen getData) { implicit request =>

      val firstName = request.userDetails.flatMap(_.firstName)
      val lastName = request.userDetails.flatMap(_.lastName)
      val nino = request.userDetails.flatMap(_.nino)

      val preparedForm =
        request.userAnswers.flatMap(_.get(CheckYourDetailsPage)) match {
          case Some(value) => form.fill(value)
          case None => form
        }

      (firstName, lastName, nino) match {
        case (Some(fn), Some(ln), Some(n)) =>
          Ok(view(preparedForm, fn, ln, n, mode))

        case _ =>
          Redirect(routes.UnauthorisedController.onPageLoad())
      }
      
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen authenticatedIdentifierAction andThen getData andThen requireData).async { implicit request =>

      val firstName = request.userDetails.flatMap(_.firstName)
      val lastName  = request.userDetails.flatMap(_.lastName)
      val nino      = request.userDetails.flatMap(_.nino)

      (firstName, lastName, nino) match {

        case (Some(fn), Some(ln), Some(n)) =>
          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, fn, ln, n, mode))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckYourDetailsPage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(CheckYourDetailsPage, mode, updatedAnswers))
          )

        case _ =>
          Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
      }
    }

}
