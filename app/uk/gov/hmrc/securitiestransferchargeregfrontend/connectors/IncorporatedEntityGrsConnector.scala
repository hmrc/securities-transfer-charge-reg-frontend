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

import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.GrsResult.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.ResourceLoader

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class IncorporatedEntityGrsConnector @Inject()(httpClient: HttpClientV2,
                                               appConfig: FrontendAppConfig,
                                               resourceLoader: ResourceLoader)
                                              (implicit ec: ExecutionContext) extends AbstractGrsConnector(httpClient):

  def retrievalUrl: String = appConfig.grsIncorporatedEntityRetrieveUrl
    
  private def initGrsJourney(initUrl: String)(implicit hc: HeaderCarrier): Future[Result] =
    super.initGrsJourney(appConfig.grsIncorporatedEntityReturnUrl)(initUrl)

  def initLimitedCompanyJourney(implicit hc: HeaderCarrier): Future[Result] =
    initGrsJourney(appConfig.initLimitedCompanyJourneyUrl)
    
  def initRegisteredSocietyJourney(implicit hc: HeaderCarrier): Future[Result] =
    initGrsJourney(appConfig.initRegisteredSocietyJourneyUrl)
  
  private val parseFailure: Throwable => GrsResult =
    xs => GrsFailure(s"Failed to parse GRS response for Incorporated Entity: ${xs.getLocalizedMessage}")

  def parseResponse(body: String): GrsResult =
    Try(Json.parse(body)).fold(parseFailure, parseSuccess)
    
  private def parseSuccess(json: JsValue): GrsResult = {
    val result = for {
      ctUtr     <- (json \ "ctutr").asOpt[String]
      regStatus <- (json \ "registration" \ "registrationStatus").asOpt[String]
      regId     <- (json \ "registration" \ "registeredBusinessPartnerId").asOpt[String]
    } yield {
      if (regStatus == REGISTERED) GrsSuccess(ctUtr, regId)
      else GrsFailure("Incorporated Entity is not registered")
    }
    result.getOrElse(GrsFailure("Failed to parse GRS response"))
  }

  override def configuration(continueUrl: String): JsValue = {
    val raw = resourceLoader.loadString("grs-incorporated-entity-config.json")
    val parsed = Json.parse(raw).as[JsObject]

    val overrideJson = Json.obj("continueUrl" -> continueUrl)
    parsed.deepMerge(overrideJson)
  }
