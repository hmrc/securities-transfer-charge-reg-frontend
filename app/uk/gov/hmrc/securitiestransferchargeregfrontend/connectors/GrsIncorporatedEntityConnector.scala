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

import play.api.libs.json.JsValue
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.GrsClient
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.ResourceLoader

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GrsIncorporatedEntityConnector @Inject()(grsClient: GrsClient,
                                               appConfig: FrontendAppConfig,
                                               resourceLoader: ResourceLoader)
                                              (implicit ec: ExecutionContext) extends AbstractGrsConnector(grsClient, resourceLoader):

  def retrievalUrl: String = appConfig.grsIncorporatedEntityRetrieveUrl
    
  private def initGrsJourney(initUrl: String)(implicit hc: HeaderCarrier): Future[Result] =
    super.initGrsJourney(appConfig.grsIncorporatedEntityReturnUrl)(initUrl)

  def initLimitedCompanyJourney(implicit hc: HeaderCarrier): Future[Result] =
    initGrsJourney(appConfig.grsLimitedCompanyJourneyUrl)
    
  def initRegisteredSocietyJourney(implicit hc: HeaderCarrier): Future[Result] =
    initGrsJourney(appConfig.grsRegisteredSocietyJourneyUrl)

  override def parseUtr(json: JsValue): Option[String] = {
    val maybeUtr = (json \ "ctutr").asOpt[String]
    logIfEmptyAndReturn("UTR", maybeUtr)
  }
  
  override def configuration(continueUrl: String): JsValue =
    super.createConfiguration("grs-incorporated-entity-config.json")(continueUrl)
  
