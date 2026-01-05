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
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.RegistrationResponse.RegistrationSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.{IndividualRegistrationDetails, RegistrationClient}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.IndividualAuth
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals.DateOfBirthRegFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualDataRequest
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.DateOfBirthRegPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.{RegistrationDataRepository, SessionRepository}
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.individuals.DateOfBirthRegView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DateOfBirthRegController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        auth: IndividualAuth,
                                        formProvider: DateOfBirthRegFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: DateOfBirthRegView,
                                        registrationClient: RegistrationClient,
                                        registrationDataRepository: RegistrationDataRepository
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

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
      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          val innerRequest = request.request
          val result = for {
            updated <- updateUserAnswers(value)
            safeId  <- registerUser(IndividualRegistrationDetails(innerRequest.firstName, None, innerRequest.lastName, value.toString, innerRequest.nino))
            _       <- registrationDataRepository.setSafeId(innerRequest.userId)(safeId)
          } yield {
              Redirect(navigator.nextPage(DateOfBirthRegPage, mode, updated))
          }
          result.recoverWith {
              // Failed to register - redirect to service error page.
            case _ => Future.successful(Redirect(routes.UpdateDobKickOutController.onPageLoad()))
          }
      )
  }

  private def updateUserAnswers[A](dob: LocalDate)(implicit request: ValidIndividualDataRequest[A]): Future[UserAnswers] = {
    request.userAnswers.set(DateOfBirthRegPage, dob).fold(
      ex => Future.failed(ex),
      updated =>
        sessionRepository.set(updated).collect {
        case true => updated
      }
    )
  }

  private def registerUser(details: IndividualRegistrationDetails): Future[String] = {
    registrationClient.register(details).collect {
      case Right(RegistrationSuccessful(safeId)) => safeId
    }
  }
}
