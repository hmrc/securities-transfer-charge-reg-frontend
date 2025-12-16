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
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.EnrolmentResponse.EnrolmentSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionResponse.SubscriptionSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.{IndividualEnrolmentDetails, IndividualSubscriptionDetails, RegistrationClient}
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.WhatsYourContactNumberFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{AlfAddress, AlfConfirmedAddress, Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.{AddressPage, WhatsYourContactNumberPage, WhatsYourEmailAddressPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.WhatsYourContactNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatsYourContactNumberController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  sessionRepository: SessionRepository,
                                                  auth: Auth,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  formProvider: WhatsYourContactNumberFormProvider,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  registrationClient: RegistrationClient,
                                                  view: WhatsYourContactNumberView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (auth.authorisedIndividualAndNotEnrolled andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(WhatsYourContactNumberPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (auth.authorisedIndividualAndNotEnrolled andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(WhatsYourContactNumberPage, value))
            _              <- sessionRepository.set(updatedAnswers)
            subscribed     <- subscribe(updatedAnswers)
            enrolled       <- enrol(request.request.maybeNino)
          } yield {
            if (subscribed && enrolled)
              Redirect(routes.RegistrationCompleteController.onPageLoad())
            else
              Redirect(routes.UpdateDobKickOutController.onPageLoad())
          }
      )
  }

  private def subscribe(userAnswers: UserAnswers): Future[Boolean] = {
    buildSubscriptionDetails(userAnswers) map { subscriptionDetails =>
      registrationClient.subscribe(subscriptionDetails).map { status =>
        status.exists(_ == SubscriptionSuccessful)
      }
    }
  }.getOrElse {
    Future.successful(false)
  }
  
  private val buildSubscriptionDetails: UserAnswers => Option[IndividualSubscriptionDetails] = { answers => for {
    alf           <- getAddress(answers)
    address        = alf.address
    (l1, l2, l3)  <- extractLines(address)
    email         <- getEmailAddress(answers)
    tel           <- getTelephoneNumber(answers)
    } yield {
      IndividualSubscriptionDetails(l1, l2, l3, address.postcode, address.country.code, tel, None, email)
    }
  }

  private val getAddress: UserAnswers => Option[AlfConfirmedAddress] = _.get[AlfConfirmedAddress](AddressPage())
  private val getEmailAddress: UserAnswers => Option[String] = _.get[String](WhatsYourEmailAddressPage)
  private val getTelephoneNumber: UserAnswers => Option[String] = _.get[String](WhatsYourContactNumberPage)

  private val extractLines: AlfAddress => Option[(String, Option[String], Option[String])] = { address =>
    val lines = address.lines
    if (lines.nonEmpty) {
      Some(lines.head, lines.lift(1), lines.lift(2))
    } else {
      None
    }
  }

  private val enrol: Option[String] => Future[Boolean] = {
    case Some(nino) =>
      registrationClient.enrolIndividual(IndividualEnrolmentDetails(nino)).map {
        case Right(EnrolmentSuccessful) => true
        case Right(other) => false
        case Left(err) => false
      }

    case None =>
      Future.successful(false)
  }

}
