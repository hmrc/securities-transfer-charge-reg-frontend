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

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.individuals

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.{RegistrationConnector, RegistrationErrorException}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.IndividualAuth
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals.DateOfBirthRegFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.DateOfBirthRegPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.DateTimeFormats.dobFormatter
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.individuals.DateOfBirthRegView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class DateOfBirthRegController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          @Named("individuals") navigator: Navigator,
                                          auth: IndividualAuth,
                                          formProvider: DateOfBirthRegFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: DateOfBirthRegView,
                                          registrationConnector: RegistrationConnector,
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport:

  import auth.*


  def onPageLoad(mode: Mode): Action[AnyContent] = (validIndividual andThen getData andThen requireData) {
    implicit request =>
      val form = formProvider()

      val preparedForm =
        request.userAnswers.get(DateOfBirthRegPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (validIndividual andThen getData andThen requireData).async {
    implicit request =>
      val innerRequest = request.request
      val registerUser = registrationConnector.registerIndividual(innerRequest.userId)(innerRequest)
      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        dateOfBirth => (
          for {
            updatedAnswers  <- Future.fromTry(request.userAnswers.set(DateOfBirthRegPage, dateOfBirth))
            nextPage        <- navigator.nextPage(DateOfBirthRegPage, mode, updatedAnswers)
            _               <- registerUser(dateOfBirth.format(dobFormatter))
          } yield Redirect(nextPage)
      ).recover {
          case _: RegistrationErrorException => Redirect(navigator.errorPage(DateOfBirthRegPage))
        }
      )
  }
