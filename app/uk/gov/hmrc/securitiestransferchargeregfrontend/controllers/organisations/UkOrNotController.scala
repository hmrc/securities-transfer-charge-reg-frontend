package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations

import play.api.data.Form
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations.UkOrNotFormProvider
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.UkOrNotPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.organisations.UkOrNotView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkOrNotController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         auth: Auth,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: UkOrNotFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: UkOrNotView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (auth.authorisedIndividualAndNotEnrolled andThen getData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(UkOrNotPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UkOrNotPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UkOrNotPage, mode, updatedAnswers))
      )
  }
}
