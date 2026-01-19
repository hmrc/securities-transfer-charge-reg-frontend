/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargeregfrontend.clients

import play.api.Logging
import play.api.http.Status.CREATED
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws
import play.api.libs.ws.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.GrsInitResult.{GrsInitFailure, GrsInitSuccess}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

enum GrsInitResult:
  case GrsInitSuccess(journeyUrl: String)
  case GrsInitFailure(reason: String)
  
object GrsInitResult extends Logging:
  def failure(reason: String): GrsInitResult = {
    logger.warn(s"GRS Initialisation failure: $reason")
    GrsInitFailure(reason)
  }
  def success(journeyUrl: String): GrsInitResult = {
    logger.info(s"GRS Initialisation succeeded: redirect URL is $journeyUrl")
    GrsInitSuccess(journeyUrl)
  }

trait GrsClient:
  def createGrsJourney(journeyCreationUrl: String)(configuration: JsValue)(implicit hc: HeaderCarrier): Future[GrsInitResult]
  def retrieveGrsResponse(retrievalUrl: String)(journeyId: String)(implicit hc: HeaderCarrier): Future[JsValue]
  
class GrsClientImpl @Inject() (httpClient: HttpClientV2)
                              (implicit ec: ExecutionContext) extends GrsClient with Logging:
  
  private type InitResponseHandler = PartialFunction[HttpResponse, GrsInitResult]

  def createGrsJourney(journeyCreationUrl: String)(configuration: JsValue)(implicit hc: HeaderCarrier): Future[GrsInitResult] =
    callGrsInit(journeyCreationUrl, configuration)
      .map(initJourneySuccessHandler.orElse(initJourneyFailureHandler))
  
  def retrieveGrsResponse(retrievalUrl: String)(journeyId: String)(implicit hc: HeaderCarrier): Future[JsValue] =
    callGrsRetrieve(retrievalUrl, journeyId)
      .map(resp => resp.json)
    
    
  private def callGrsInit(initUrl: String, payload: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Initiating GRS journey using URL: $initUrl")
    httpClient
      .post(url"$initUrl")
      .withBody(payload)
      .execute[HttpResponse]
  }

  private val parseInitJourneyResponse: String => Option[String] = { body =>
    Try(Json.parse(body).asInstanceOf[JsObject]) match {
      case Success(json) => (json \ "journeyStartUrl").asOpt[String]
      case Failure(xs)   =>
        logger.warn("GRS Initialisation response could not be parsed as JSON")
        None
    }
  }
  
  private val initJourneySuccessHandler: InitResponseHandler =
    case resp if resp.status == CREATED =>
      parseInitJourneyResponse(resp.body)
        .map(journeyUrl => GrsInitResult.success(journeyUrl))
        .getOrElse {
          GrsInitResult.failure("Could not find journey URL in GRS response body")
        }

  private val initJourneyFailureHandler: InitResponseHandler =
    resp => GrsInitResult.failure("Unexpected HTTP return code: " + resp.status)

  private def callGrsRetrieve(retrievalUrl: String, journeyId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val grsRetrievalUrl = url"$retrievalUrl/$journeyId"
    logger.info(s"Calling GRS journey result retrieval URL at $retrievalUrl")
    httpClient
      .get(grsRetrievalUrl)
      .execute[HttpResponse]
  }
  

