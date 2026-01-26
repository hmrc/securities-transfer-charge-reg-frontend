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

package uk.gov.hmrc.securitiestransferchargeregfrontend.controllers

import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

/*
 * This class holds common redirects used across the controllers.
 */
@Singleton
class Redirects @Inject()(appConfig: FrontendAppConfig)  {

  val toFuture: Result => Future[Result] = result => Future.successful(result)
  val unauthorisedPath: String = routes.UnauthorisedController.onPageLoad().url
  
  val redirectToLogin: Result = Redirect(appConfig.unauthorisedUrl)
  def redirectToRegisterIndividualF: Future[Result] = toFuture(Redirect(appConfig.registerIndividualUrl))
  def redirectToIVUpliftF: Future[Result] = toFuture(Redirect(appConfig.ivUpliftUrl))
  def redirectToRegisterOrganisationF: Future[Result] = toFuture(Redirect(appConfig.registerOrganisationUrl))
  def redirectToAsaF: Future[Result] = toFuture(Redirect(appConfig.asaUrl))
  def redirectToRegisterF: Future[Result] = toFuture(Redirect(appConfig.registerUrl))
  def redirectToService: Result = Redirect(appConfig.stcServiceUrl)
  def redirectToServiceF: Future[Result] = toFuture(redirectToService)
  
  // TODO: This redirect needs to be updated when the correct KO page is available - see STOSB-1348
  def redirectToAssistantKOPageF: Future[Result] = toFuture(Redirect(individuals.routes.UpdateDobKickOutController.onPageLoad()))

}