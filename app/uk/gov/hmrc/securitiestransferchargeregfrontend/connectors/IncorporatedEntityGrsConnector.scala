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

import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try
import GrsResult.*

class IncorporatedEntityGrsConnector @Inject()(ws: WSClient,
                                               appConfig: FrontendAppConfig)
                                              (implicit ec: ExecutionContext) extends AbstractGrsConnector(ws) {

  val initLimitedCompanyJourney = s"${appConfig.grsIncorporatedEntityBaseUrl}/api/limited-company-journey"
  val initRegisteredSocietyJourney = s"${appConfig.grsIncorporatedEntityBaseUrl}/api/registered-society-journey"
  
  def configuration(continueUrl: String): JsValue = Json.parse(
    s"""
      |{
      |  "continueUrl" : "$continueUrl",
      |  "businessVerificationCheck": false,
      |  "optServiceName" : "Securities Transfer Charge",
      |  "deskProServiceId" : "STC",
      |  "signOutUrl" : "/account/sign-out",
      |  "regime" : "STC",
      |  "accessibilityUrl" : "/accessibility-statement/my-service",
      |  "labels": {
      |    "cy": {
      |      "optServiceName": "Service name translated into welsh"
      |    },
      |    "en": {
      |      "optServiceName": "Securities Transfer Charge"
      |    }
      |  }
      |}""".stripMargin
  )

  def retrievalUrl: String = appConfig.grsIncorporatedEntityRetrieveUrl
    
  private val parseFailure: Throwable => GrsResult =
    _ => GrsFailure("Failed to parse GRS response for Incorporated Entity")

  def parseResponse(body: String): GrsResult =
    Try(Json.parse(body)).fold(parseFailure, parseSuccess)
    
  private def parseSuccess(json: JsValue): GrsResult = {
    val result = for {
      ctUtr     <- (json \ "ctutr").asOpt[String]
      regStatus <- (json \ "registration" \ "registrationStatus").asOpt[String]
      regId     <- (json \ "registration" \ "registeredBusinessPartnerId").asOpt[String]
      if regStatus == "REGISTERED"
    } yield GrsSuccess(ctUtr, regId)
    result.getOrElse(GrsFailure("Incorporated Entity is not registered"))
  }
}

  