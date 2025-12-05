package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.api.libs.ws.{WSClient, WSRequest}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.Auth
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.views.html.RegForSecuritiesTransferChargeView

import javax.inject.Inject
import scala.concurrent.ExecutionContext


class AddressController @Inject()( auth: Auth,
                                   navigator: Navigator,
                                   val controllerComponents: MessagesControllerComponents,
                                   ws: WSClient,
                                   config: FrontendAppConfig
                                 ) (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  private val redirectToErrorPage: Result = Redirect("route/to/error/page")
  private type ResponseHandler = PartialFunction[WSRequest#Self#Response, Result]
  
  def onPageLoad: Action[AnyContent] = (auth.authorisedAndNotEnrolled).async {
    implicit request =>
      initAlfJourneyRequest()
        .map(journeySuccess.orElse(journeyFailure)(_))
  }

  def onReturn(key: String): Action[AnyContent] = (auth.authorisedAndNotEnrolled) {
    implicit request =>
      alfRetrieveAddress(key).map(retrievalSuccess.orElse(retrievalFailure)(_))
  }

  private def alfRetrieveAddress(key: String) = {
    val retrieveAddress = s"${config.alfRetrieveUrl}?id=$key"
    ws
      .url(retrieveAddress)
      .get()
  }
  
  private def retrievalSuccess: ResponseHandler = {
    
  }
  private def journeySuccess: ResponseHandler = {
    case resp if resp.status == NO_CONTENT =>
      val maybeAddressLookupJourney = resp.header("Location")
      maybeAddressLookupJourney.map(Redirect(_)).getOrElse {
        logger.warn("Address lookup initiation did not return a Location header")
        redirectToErrorPage
      }
  }
  
  private def journeyFailure: ResponseHandler = {
    case resp =>
      logger.warn(s"Address lookup initiation failed with status ${resp.status}")
      redirectToErrorPage
  }
  
  private def initAlfJourneyRequest() = {
    ws
      .url(config.alfUrl)
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(payload)
  }
  
  val payload: JsValue = {
    // Implementation to create the payload
    Json.parse("""{
  "version" : 2,
  "options" : {
    "continueUrl" : "This will be ignored",
    "useNewGovUkServiceNavigation" : false
  },
  "labels" : {
    "en" : {
      "appLevelLabels" : {
        "navTitle" : "Address Lookup Example"
      },
      "selectPageLabels" : { },
      "lookupPageLabels" : { },
      "editPageLabels" : { },
      "confirmPageLabels" : { },
      "countryPickerLabels" : { },
      "international" : { },
      "otherLabels" : { }
    },
    "cy" : {
      "appLevelLabels" : {
        "navTitle" : "Address Lookup Example (Welsh)"
      },
      "selectPageLabels" : { },
      "lookupPageLabels" : { },
      "editPageLabels" : { },
      "confirmPageLabels" : { },
      "countryPickerLabels" : { },
      "international" : { },
      "otherLabels" : { }
    }
  }
}""")
  }
}
