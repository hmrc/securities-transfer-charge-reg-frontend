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
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.SubscriptionConnector
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.OrgAuth
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations.routes.RegistrationCompleteController
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations.ContactNumberFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.ContactNumberPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.ContactNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactNumberController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         auth: OrgAuth,
                                         formProvider: ContactNumberFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: ContactNumberView,
                                         subscriptionConnector: SubscriptionConnector,
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  import auth.*

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (validOrg andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ContactNumberPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (validOrg andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ContactNumberPage, value))
            _ <- sessionRepository.set(updatedAnswers)
            _ <- subscriptionConnector.subscribeAndEnrolOrganisation(request.request.userId)(updatedAnswers)
          } yield Redirect(RegistrationCompleteController.onPageLoad())
      ).recover {
        case _ => Redirect(JourneyRecoveryController.onPageLoad())
      }
  }
}
