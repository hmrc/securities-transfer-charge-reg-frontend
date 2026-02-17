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
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations.UkOrNotFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.UkOrNotPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.UkOrNotView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class UkOrNotController @Inject()(
                                   override val messagesApi: MessagesApi,
                                   @Named("organisations") navigator: Navigator,
                                   auth: OrgAuth,
                                   formProvider: UkOrNotFormProvider,
                                   val controllerComponents: MessagesControllerComponents,
                                   view: UkOrNotView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import auth.*

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (validOrg andThen getData).async {
    implicit request =>

      val preparedForm = request.userAnswers
          .flatMap(_.get(UkOrNotPage))
          .fold(form)(form.fill)

      request.userAnswers match {

        case Some(ua) => navigator.previousPage(UkOrNotPage, mode, ua).map {
        backLinkCall =>
              Ok(view(preparedForm, mode, backLinkCall))
            }

        case None => Future.successful(Ok(view(preparedForm, mode, routes.RegForSecuritiesTransferChargeController.onPageLoad())))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold[Future[play.api.mvc.Result]](

        formWithErrors =>
          navigator
            .previousPage(UkOrNotPage, mode, request.userAnswers)
            .map { backLinkCall =>
              BadRequest(view(formWithErrors, mode, backLinkCall))
            },

        isUk =>
          for {
            updatedAnswers <- Future.fromTry(
              request.userAnswers.set(UkOrNotPage, isUk)
            )
            nextPage <- navigator.nextPage(
              UkOrNotPage,
              mode,
              updatedAnswers
            )
          } yield Redirect(nextPage)
      )
    }
}