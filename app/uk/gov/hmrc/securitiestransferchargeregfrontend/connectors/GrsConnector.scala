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
import play.api.libs.json.*
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.GrsClient
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.GrsInitResult.{GrsInitFailure, GrsInitSuccess}
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.ResourceLoader

import scala.concurrent.{ExecutionContext, Future}

enum GrsResult:
  case GrsSuccess(utr: String, safeId: String)
  case GrsFailure(reason: String)

object GrsResult extends Logging:
  def success(utr: String, safeId: String): GrsResult = {
    logger.info("GRS journey data successfully retrieved")
    GrsSuccess(utr, safeId)
  }
  
  def failure(reason: String): GrsResult = {
    logger.warn(s"GRS journey data retrieval failed: $reason")
    GrsFailure(reason)
  }
  
final class GrsException(msg: String) extends RuntimeException(msg)

abstract class AbstractGrsConnector(grsClient: GrsClient,
                                    resourceLoader: ResourceLoader)
                                   (implicit ec: ExecutionContext) extends Logging {

  def configuration(continueUrl: String): JsValue

  def retrievalUrl: String
  
  private val REGISTERED = "REGISTERED"
  
  def initGrsJourney(continueUrl: String)(initUrl: String)(implicit hc: HeaderCarrier): Future[Result] =
    grsClient.createGrsJourney(initUrl)(configuration(continueUrl))
      .flatMap {
        case GrsInitSuccess(url) => Future.successful(Redirect(url))
        case GrsInitFailure(msg) => Future.failed(GrsException(msg))
      }

  def retrieveGrsResults(journeyId: String)(implicit hc: HeaderCarrier): Future[GrsResult] =
    grsClient.retrieveGrsResponse(retrievalUrl)(journeyId)
      .map(json => parseGrsRetrievalData(json))

  protected[connectors] def logIfEmptyAndReturn[A](attributeName: String, maybeAttribute: Option[A]): Option[A] = {
    if (maybeAttribute.isEmpty) logger.info(s"Failed to find a $attributeName in the GRS response.")
    maybeAttribute
  }

  def parseUtr(json: JsValue): Option[String] = {
    val maybeUtr = (json \ "sautr").asOpt[String]
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

  def parseGrsRetrievalData(json: JsValue): GrsResult = {
    logger.info(s"Parsing GRS response $json")
    val result = for {
      utr       <- parseUtr(json)
      regStatus <- parseRegistrationStatus(json)
      regId     <- parseRegistrationId(json)
    } yield {
      if (regStatus == REGISTERED) GrsResult.success(utr, regId)
      else GrsResult.failure("Organisation is not registered")
    }
    result.getOrElse(GrsResult.failure("Failed to parse GRS response"))
  }

  def createConfiguration(filename: String)(continueUrl: String): JsValue = {
    val raw = resourceLoader.loadString(filename)
    val parsed = Json.parse(raw).as[JsObject]

    val overrideJson = Json.obj("continueUrl" -> continueUrl)
    logger.info("Continue URL for GRS journey: " + continueUrl)
    parsed.deepMerge(overrideJson)
  }
}
