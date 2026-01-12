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

package uk.gov.hmrc.securitiestransferchargeregfrontend.connectors

import play.api.Logging
import play.api.http.Status.CREATED
import play.api.libs.json.*
import play.api.libs.ws.*
import play.api.mvc.*
import play.api.mvc.Results.Redirect

import scala.concurrent.{ExecutionContext, Future}

enum GrsResult:
  case GrsSuccess(ctUtr: String, safeId: String)
  case GrsFailure(reason: String)


trait GrsConnector:
  def initGrsJourney(initUrl: String, continueUrl: String): Future[Result]
  def retrieveGrsResults(journeyId: String): Future[GrsResult]
  def configuration(continueUrl: String): JsValue
  def retrievalUrl: String
  def parseResponse(body: String): GrsResult
  
final class GrsException(msg: String) extends RuntimeException(msg)

abstract class AbstractGrsConnector(ws: WSClient)
                               (implicit ec: ExecutionContext) extends GrsConnector with Logging {

  private type ResponseHandler = PartialFunction[WSResponse, Result]

  override def initGrsJourney(initUrl: String, continueUrl: String): Future[Result] =
    callGrsInit(initUrl, continueUrl)
      .map(journeySuccess.orElse(journeyFailure))

  private def callGrsInit(initUrl: String, continueUrl: String): Future[WSResponse] =
    ws
      .url(initUrl)
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(configuration(continueUrl))

  private def journeySuccess: ResponseHandler =
    case resp if resp.status == CREATED =>
      parse(resp.body)
        .map(Redirect(_)).getOrElse {
          failure(s"Could not find journey URL in GRS response body")
        }

  private val parse: String => Option[String] = { body =>
    val json = Json.parse(body).asInstanceOf[JsObject]
    (json \ "journeyStartUrl").asOpt[String]
  }

  private val failure: String => Nothing = { fullMessage =>
    logger.error(fullMessage)
    throw new GrsException(fullMessage)
  }

  private val journeyFailure: ResponseHandler = { resp =>
    failure("Address lookup initiation failed: " + resp.status)
  }

  override def retrieveGrsResults(journeyId: String): Future[GrsResult] =
    callGrsRetrieve(journeyId)
      .map(resp => parseResponse(resp.body))

  private def callGrsRetrieve(journeyId: String): Future[WSResponse] =
    ws
      .url(s"$retrievalUrl/$journeyId")
      .get()
  
}
