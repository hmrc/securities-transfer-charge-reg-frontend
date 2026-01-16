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
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.ResourceLoader

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GrsPartnershipConnector @Inject()(httpClient: HttpClientV2,
                                        appConfig: FrontendAppConfig,
                                        resourceLoader: ResourceLoader)
                                       (implicit ec: ExecutionContext) extends AbstractGrsConnector(httpClient):

  def retrievalUrl: String = appConfig.grsPartnershipRetrieveUrl

  private def initGrsJourney(initUrl: String)(implicit hc: HeaderCarrier): Future[Result] =
    super.initGrsJourney(appConfig.grsPartnershipReturnUrl)(initUrl)

  def initLimitedPartnershipJourney(implicit hc: HeaderCarrier): Future[Result] =
    initGrsJourney(appConfig.grsLimitedPartnershipJourneyUrl)

  def initScottishLimitedPartnershipJourney(implicit hc: HeaderCarrier): Future[Result] =
    initGrsJourney(appConfig.grsScottishLimitedPartnershipJourneyUrl)

  def initLimitedLiabilityPartnershipJourney(implicit hc: HeaderCarrier): Future[Result] =
    initGrsJourney(appConfig.grsLimitedLiabilityPartnershipJourneyUrl)

  override def parseUtr(json: JsValue): Option[String] = {
    val maybeUtr = (json \ "sautr").asOpt[String]
    logIfEmptyAndReturn("UTR", maybeUtr)
  }
  
  override def configuration(continueUrl: String): JsValue = {
    val raw = resourceLoader.loadString("grs-partnership-config.json")
    val parsed = Json.parse(raw).as[JsObject]

    val overrideJson = Json.obj("continueUrl" -> continueUrl)
    parsed.deepMerge(overrideJson)
  }
