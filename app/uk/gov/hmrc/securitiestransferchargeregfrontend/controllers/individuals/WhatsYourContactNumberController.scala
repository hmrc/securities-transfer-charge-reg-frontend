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

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals.WhatsYourContactNumberFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.WhatsYourContactNumberPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.individuals.WhatsYourContactNumberView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class WhatsYourContactNumberController @Inject()( override val messagesApi: MessagesApi,
                                                  auth: IndividualAuth,
                                                  @Named("individuals") navigator: Navigator,
                                                  formProvider: WhatsYourContactNumberFormProvider,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  subscriptionConnector: SubscriptionConnector,
                                                  view: WhatsYourContactNumberView
                                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport:

  import auth.*
  
  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (validIndividual andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(WhatsYourContactNumberPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (validIndividual andThen getData andThen requireData).async { implicit request =>

      val innerRequest = request.request
      val subscribe = subscriptionConnector.subscribeAndEnrolIndividual(innerRequest.userId)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        contactNumber => (
          for {
            updatedAnswers  <- Future.fromTry(request.userAnswers.set(WhatsYourContactNumberPage, contactNumber))
            nextPage        <- navigator.nextPage(WhatsYourContactNumberPage, mode, updatedAnswers)
            _               <- subscribe(updatedAnswers, innerRequest)
          } yield Redirect(nextPage)
        ).recover { 
          case _: RegistrationDataNotFoundException | _: SubscriptionErrorException | _: EnrolmentErrorException
            => Redirect(navigator.errorPage(WhatsYourContactNumberPage))
        }
      )
    }

