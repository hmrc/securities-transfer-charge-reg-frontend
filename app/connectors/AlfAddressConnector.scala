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
import play.api.libs.json.*
import play.api.libs.ws.*
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.AlfConfirmedAddress
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.ResourceLoader

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


trait AlfAddressConnector {
  def initAlfJourneyRequest(): Future[Result]
  def alfRetrieveAddress(key: String): Future[AlfConfirmedAddress]
}

class AlfAddressConnectorImpl @Inject() ( ws: WSClient,
                                      config: FrontendAppConfig,
                                      resourceLoader: ResourceLoader)
                                    ( implicit ec: ExecutionContext) extends AlfAddressConnector with Logging {

  private type ResponseHandler = PartialFunction[WSResponse, Result]
  private[connectors] final class AlfException(msg: String) extends RuntimeException(msg)

  def initAlfJourneyRequest(): Future[Result] = {
    callAlfInit().map(journeySuccess.orElse(journeyFailure)(_))
  }

  def alfRetrieveAddress(key: String): Future[AlfConfirmedAddress] = {
    callAlfRetrieve(key).map(retrievalSuccess)
  }

  private def callAlfInit(): Future[WSResponse] = {
    ws
      .url(config.alfInitUrl)
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(payload)
  }

  private def journeySuccess: ResponseHandler = {
    case resp if resp.status == ACCEPTED =>
      val maybeAddressLookupJourney = resp.header("Location")
      maybeAddressLookupJourney.map(Redirect(_)).getOrElse {
        failure(s"Address lookup initiation did not return a Location header - ${resp.status}")
      }
  }
  
  private val failure: String => Nothing = { fullMessage =>
    logger.error(fullMessage)
    throw new AlfException(fullMessage)
  }

  private val journeyFailure: ResponseHandler = { _ =>
    failure("Address lookup initiation failed")
  }
  
  private def callAlfRetrieve(key: String): Future[WSResponse] = {
    val retrieveAddress = s"${config.alfRetrieveUrl}?id=$key"
    ws
      .url(retrieveAddress)
      .get()
  }

  private def retrievalSuccess[A](resp: WSResponse): AlfConfirmedAddress = {
    resp.json.validate[AlfConfirmedAddress].getOrElse {
      failure("Could not retrieve the address from ALF")
    }
  }


  private def payload: JsValue = {
    val raw = resourceLoader.loadString("alf.json")
    val parsed = Json.parse(raw).as[JsObject]
    
    val overrideJson = Json.obj(
      "options" -> Json.obj(
        "continueUrl" -> config.alfContinueUrl
      )
    )

    val finalPayload = parsed.deepMerge(overrideJson)

    finalPayload
  }


}


