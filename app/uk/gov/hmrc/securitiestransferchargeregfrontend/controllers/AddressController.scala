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
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.actions.{Auth, DataRetrievalAction}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.OptionalDataRequest
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.AddressPage

import javax.inject.Inject
import scala.concurrent.ExecutionContext


class AddressController @Inject()( auth: Auth,
                                   navigator: Navigator,
                                   val controllerComponents: MessagesControllerComponents,
                                   ws: WSClient,
                                   config: FrontendAppConfig,
                                   getData: DataRetrievalAction
                                 ) (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  private val redirectToErrorPage: Result = Redirect("route/to/error/page")
  private type ResponseHandler = PartialFunction[WSRequest#Self#Response, Result]
  
  /*
   * Creates an address journey and redirects to the to it.
   * If the journey fails to initialise, the user is sent to an error page.
   */
  def onPageLoad: Action[AnyContent] = auth.authorisedAndNotEnrolled.async {
    implicit request =>
      initAlfJourneyRequest()
        .map(journeySuccess.orElse(journeyFailure)(_))
  }

  /*
   * Retrieves the outcome of the journey and stores the address in UserAnswers if
   * it was successful. If retrieval fails the user is sent to an error page.
   */
  def onReturn(key: String): Action[AnyContent] = (auth.authorisedAndNotEnrolled andThen getData).async {
    implicit request =>
      logger.warn("BACK IN SERVICE")
      alfRetrieveAddress(key).map(retrievalSuccess.orElse(retrievalFailure)(_))
  }

  private def initAlfJourneyRequest() = {
    ws
      .url(config.alfUrl)
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(payload)
  }

  private def journeySuccess: ResponseHandler = {
    case resp if resp.status == ACCEPTED =>
      val maybeAddressLookupJourney = resp.header("Location")
      maybeAddressLookupJourney.map(Redirect(_)).getOrElse {
        failure("Address lookup initiation did not return a Location header")(resp)
      }
  }
  
  
  private def failure(msg: String): ResponseHandler = {
    case resp =>
      logger.warn(msg + s" - status ${resp.status}")
      redirectToErrorPage
  }
  
  private def journeyFailure = failure("Address lookup initiation failed")

  private def alfRetrieveAddress(key: String) = {
    val retrieveAddress = s"${config.alfRetrieveUrl}?id=$key"
    ws
      .url(retrieveAddress)
      .get()
  }
  
  private def retrievalSuccess[A](implicit request: OptionalDataRequest[A]): ResponseHandler = {
    case resp => resp.json.validate[AlfConfirmedAddress].map {
      address => updateUserAnswers(address)
      // Go to the next page? Or show the address?
      Ok
    }.getOrElse {
      redirectToErrorPage
    }
  }
      
  private def retrievalFailure = failure("Could not retrieve the address entered by the user")
  
  private def updateUserAnswers[A](value: AlfConfirmedAddress)(implicit request: OptionalDataRequest[A]): Unit = {
     logger.warn("Got address: " + value.toString)
      val updatedAnswers = request.userAnswers
        .getOrElse(UserAnswers(request.userId))
        .set(AddressPage[AlfConfirmedAddress](), value)
        .get

      getData.sessionRepository.set(updatedAnswers).map { _ =>
        Redirect(navigator.nextPage(AddressPage(), NormalMode, updatedAnswers))
    }
  }

  case class AlfConfirmedAddress( auditRef: String,
                                  id: Option[String],
                                  address: AlfAddress)

  object AlfConfirmedAddress:
    given Reads[AlfConfirmedAddress] = Json.reads[AlfConfirmedAddress]
    given Writes[AlfConfirmedAddress] = Json.writes[AlfConfirmedAddress]

  case class AlfAddress(lines: List[String],
                        postcode: String,
                        country: Country)
  
  object AlfAddress:
    given Reads[AlfAddress] = Json.reads[AlfAddress]
    given Writes[AlfAddress] = Json.writes[AlfAddress]
    
  case class Country( code: String,
                      name: String)
  object Country:
    given Reads[Country] = Json.reads[Country]
    given Writes[Country] = Json.writes[Country]


  private val payload: JsValue = {
    // Implementation to create the payload
    Json.parse("""{
  "version" : 2,
  "options" : {
    "continueUrl" : "http://localhost:9000/register-securities-transfer-charge/address/return",
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
