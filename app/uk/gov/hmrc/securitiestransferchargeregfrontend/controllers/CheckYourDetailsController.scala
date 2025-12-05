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

import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.CheckYourDetailsFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.StcAuthRequest
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
                                            view: CheckYourDetailsView,
                                            config: FrontendAppConfig
                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (auth.authorisedIndividualAndNotEnrolled andThen getData) { implicit request =>
      val preparedForm = request.userAnswers
        .flatMap(_.get(CheckYourDetailsPage))
        .fold(form)(form.fill)

      extractData(request.request)
        .map {
          case (fn, ln, nino) => Ok(view(preparedForm, fn, ln, nino, mode))
        }.getOrElse(noAuthDetails)
      }
    


  def onSubmit(mode: Mode): Action[AnyContent] = {
    (auth.authorisedIndividualAndNotEnrolled andThen getData).async { implicit request =>

      extractData(request.request).map { case (fn, ln, nino) =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, fn, ln, nino, mode))),
          value => {
            val updatedAnswers =
              request.userAnswers
                .getOrElse(UserAnswers(request.userId))
                .set(CheckYourDetailsPage, value)
                .get

            sessionRepository.set(updatedAnswers).map { _ =>
              Redirect(navigator.nextPage(CheckYourDetailsPage, mode, updatedAnswers))
            }
          })
      }.getOrElse(Future.successful(noAuthDetails))
    }
  }
  
  private def noAuthDetails: Result = {
    logger.warn("CheckYourDetailsController onPageLoad: missing user details in auth request")
    Redirect(config.ivUpliftUrl)
  }
  
  private def extractData[A](request: StcAuthRequest[A]): Option[(String, String, String)] = {
    for {
      firstName <- request.maybeName.flatMap(_.givenName)
      lastName <- request.maybeName.flatMap(_.familyName)
      nino <- request.maybeNino
    } yield {
      (firstName, lastName, nino)
    }
  }
  
}
