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
import play.api.libs.ws
import play.api.libs.ws.*
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsResult.{GrsFailure, GrsSuccess}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

enum GrsResult:
  case GrsSuccess(ctUtr: String, safeId: String)
  case GrsFailure(reason: String)


trait GrsConnector:
  def initGrsJourney(continueUrl: String)(initUrl: String)(implicit hc: HeaderCarrier): Future[Result]
  def retrieveGrsResults(journeyId: String)(implicit hc: HeaderCarrier): Future[GrsResult]
  def configuration(continueUrl: String): JsValue
  def retrievalUrl: String
  def parseResponse(body: String): GrsResult
  
final class GrsException(msg: String) extends RuntimeException(msg)

abstract class AbstractGrsConnector(httpClient: HttpClientV2)
                                   (implicit ec: ExecutionContext) extends GrsConnector with Logging {

  private type ResponseHandler = PartialFunction[HttpResponse, Result]
  val REGISTERED = "REGISTERED"
  
  override def initGrsJourney(continueUrl: String)(initUrl: String)(implicit hc: HeaderCarrier): Future[Result] =
    callGrsInit(initUrl, continueUrl)
      .map(initJourneySuccess.orElse(initJourneyFailure))

  private def callGrsInit(initUrl: String, continueUrl: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Initiating GRS journey using URL: $initUrl")
    httpClient
      .post(url"$initUrl")
      .withBody(configuration(continueUrl))
      .execute[HttpResponse]
  }

  private def initJourneySuccess: ResponseHandler =
    case resp if resp.status == CREATED =>
      parseInitJourneyResponse(resp.body)
        .map(Redirect(_)).getOrElse {
          failure(s"Could not find journey URL in GRS response body")
        }

  private val parseInitJourneyResponse: String => Option[String] = { body =>
    val json = Json.parse(body).asInstanceOf[JsObject]
    (json \ "journeyStartUrl").asOpt[String]
  }

  private val failure: String => Nothing = { fullMessage =>
    logger.error(fullMessage)
    throw new GrsException(fullMessage)
  }

  private val initJourneyFailure: ResponseHandler = { resp =>
    failure("GRS initiation failed: " + resp.status)
  }

  override def retrieveGrsResults(journeyId: String)(implicit hc: HeaderCarrier): Future[GrsResult] =
    callGrsRetrieve(journeyId)
      .map(resp => parseResponse(resp.body))

  private def callGrsRetrieve(journeyId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .get(url"$retrievalUrl/$journeyId")
      .execute[HttpResponse]

  private val parseFailure: Throwable => GrsResult =
    xs => GrsFailure(s"Failed to parse GRS response: ${xs.getLocalizedMessage}")

  def parseResponse(body: String): GrsResult =
    Try(Json.parse(body)).fold(parseFailure, parseSuccess)

  protected[connectors] def logIfEmptyAndReturn[A](attributeName: String, maybeAttribute: Option[A]): Option[A] = {
    if (maybeAttribute.isEmpty) logger.info(s"Failed to find a $attributeName in the GRS response.")
    maybeAttribute
  }

  def parseUtr(json: JsValue): Option[String] = {
    val maybeUtr = (json \ "ctutr").asOpt[String]
    logIfEmptyAndReturn("UTR", maybeUtr)
  }
  
  def parseRegistrationStatus(json: JsValue): Option[String] = {
    val maybeStatus = (json \ "registration" \ "registrationStatus").asOpt[String]
    logIfEmptyAndReturn("Registration Status", maybeStatus)
  }

  def parseRegistrationId(json: JsValue): Option[String] = {
    val maybeId = (json \ "registration" \ "registeredBusinessPartnerId").asOpt[String]
    logIfEmptyAndReturn("Registration Id", maybeId)
  }
  
  def parseSuccess(json: JsValue): GrsResult = {
    val result = for {
      utr       <- parseUtr(json)
      regStatus <- parseRegistrationStatus(json)
      regId     <- parseRegistrationId(json)
    } yield {
      if (regStatus == REGISTERED) GrsSuccess(utr, regId)
      else GrsFailure("Partnership is not registered")
    }
    result.getOrElse(GrsFailure("Failed to parse GRS response"))
  }
}
