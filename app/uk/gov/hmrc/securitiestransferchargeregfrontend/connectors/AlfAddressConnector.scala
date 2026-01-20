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

package uk.gov.hmrc.securitiestransferchargeregfrontend.connectors

import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.json.*
import play.api.libs.ws.*
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.AlfConfirmedAddress
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.ResourceLoader

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait AlfAddressConnector {
  def initAlfJourneyRequest(returnUrl: String)(implicit hc: HeaderCarrier): Future[Result]
  def alfRetrieveAddress(key: String)(implicit hc: HeaderCarrier): Future[AlfConfirmedAddress]
}

class AlfAddressConnectorImpl @Inject() ( http: HttpClientV2,
                                          config: FrontendAppConfig,
                                          resourceLoader: ResourceLoader)
                                        ( implicit ec: ExecutionContext) extends AlfAddressConnector with Logging {

  private type ResponseHandler = PartialFunction[HttpResponse, Result]
  private[connectors] final class AlfException(msg: String) extends RuntimeException(msg)

  def initAlfJourneyRequest(returnUrl: String)(implicit hc: HeaderCarrier): Future[Result] = {
    callAlfInit(returnUrl).map(journeySuccess.orElse(journeyFailure)(_))
  }

  def alfRetrieveAddress(key: String)(implicit hc: HeaderCarrier): Future[AlfConfirmedAddress] = {
    callAlfRetrieve(key).map(retrievalSuccess)
  }

  private def callAlfInit(returnUrl: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http
      .post(url"${config.alfInitUrl}")
      .withBody(payload(returnUrl))
      .setHeader("Content-Type" -> "application/json")
      .execute[HttpResponse]
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
  
  private def callAlfRetrieve(key: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val retrieveAddress = s"${config.alfRetrieveUrl}?id=$key"
    http
      .get(url"$retrieveAddress")
      .execute[HttpResponse]
  }

  private def retrievalSuccess[A](resp: HttpResponse): AlfConfirmedAddress = {
    resp.json.validate[AlfConfirmedAddress].getOrElse {
      failure("Could not retrieve the address from ALF")
    }
  }


  private def payload(returnUrl: String): JsValue = {
    val raw = resourceLoader.loadString("alf.json")
    val parsed = Json.parse(raw).as[JsObject]
    
    val overrideJson = Json.obj(
      "options" -> Json.obj(
        "continueUrl" -> returnUrl
      )
    )
    parsed.deepMerge(overrideJson)
  }

}


