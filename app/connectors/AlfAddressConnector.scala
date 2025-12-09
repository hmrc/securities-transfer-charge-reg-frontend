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

package connectors

import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.*
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.AlfConfirmedAddress

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AlfAddressConnector @Inject() ( ws: WSClient,
                                      config: FrontendAppConfig)
                                    ( implicit ec: ExecutionContext) extends Logging {

  private type ResponseHandler = PartialFunction[WSRequest#Self#Response, Result]
  private val redirectToErrorPage: Result = Redirect("route/to/error/page")

  def initAlfJourneyRequest(): Future[Result] = {
    callAlfInit().map(journeySuccess.orElse(journeyFailure)(_))
  }

  private def callAlfInit(): Future[WSResponse] = {
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
  
  def alfRetrieveAddress(key: String): Future[Option[AlfConfirmedAddress]] = {
    callAlfRetrieve(key).map(retrievalSuccess)
  }
  
  def callAlfRetrieve(key: String): Future[WSResponse] = {
    val retrieveAddress = s"${config.alfRetrieveUrl}?id=$key"
    ws
      .url(retrieveAddress)
      .get()
  }

  private def retrievalSuccess[A](resp: WSResponse): Option[AlfConfirmedAddress] = {
    resp.json.validate[AlfConfirmedAddress].asOpt
  }

  
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


