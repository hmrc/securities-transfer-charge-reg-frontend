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
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.{extractData, routes}
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals.DateOfBirthRegFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.DataRequest
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.DateOfBirthRegPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.individuals.DateOfBirthRegView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DateOfBirthRegController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        auth: Auth,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: DateOfBirthRegFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: DateOfBirthRegView,
                                        registrationClient: RegistrationClient
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (auth.authorisedIndividualAndNotEnrolled andThen getData andThen requireData) {
    implicit request =>
      val form = formProvider()

      val preparedForm =
        request.userAnswers.get(DateOfBirthRegPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (auth.authorisedIndividualAndNotEnrolled andThen getData andThen requireData).async {
    implicit request =>
      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          extractData(request.request).map { data =>
            for {
              updated <- updateUserAnswers(value)
              registered <- registerUser(IndividualRegistrationDetails(data._1, None, data._2, value.toString, data._3))
            } yield {
              if (registered) {
                // Success - redirect to the next page.
                Redirect(navigator.nextPage(DateOfBirthRegPage, mode, updated))
              } else {
                // Failed to register - redirect to service error page.
                Redirect(routes.UpdateDobKickOutController.onPageLoad())
              }
            }
          }.getOrElse {
            // Failed to extract data - this should not happen - redirect to global error page
            // This will go away when we merge the no-option branch!
            throw new RuntimeException("Unexpected empty option")
          }
      )
  }

  private def updateUserAnswers[A](dob: LocalDate)(implicit request: DataRequest[A]): Future[UserAnswers] = {
    request.userAnswers.set(DateOfBirthRegPage, dob) match {
      case Success(updated) => sessionRepository.set(updated).collect {
        case true => updated
      }
      case Failure(exception) => throw exception
    }
  }

  private def registerUser(details: IndividualRegistrationDetails): Future[Boolean] = {
    registrationClient.register(details).map {
      _ == Right(RegistrationSuccessful)
    }
  }
  }
