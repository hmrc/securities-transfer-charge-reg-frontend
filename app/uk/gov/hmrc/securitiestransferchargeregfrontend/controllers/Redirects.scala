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

import play.api.mvc.Results.Redirect
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig

import javax.inject.{Inject, Singleton}

/*
 * This class holds common redirects used across the controllers.
 */
@Singleton
class Redirects @Inject()(appConfig: FrontendAppConfig)  {

  val redirectToLogin: play.api.mvc.Result = Redirect(appConfig.loginUrl)
  val redirectToRegisterIndividual: play.api.mvc.Result = Redirect(appConfig.registerIndividualUrl)
  val redirectToIVUplift: play.api.mvc.Result = Redirect(appConfig.ivUpliftUrl)
  val redirectToRegisterOrganisation: play.api.mvc.Result = Redirect(appConfig.registerOrganisationUrl)
  val redirectToASA: play.api.mvc.Result = Redirect(appConfig.asaUrl)
  val redirectToRegister: play.api.mvc.Result = Redirect(appConfig.registerUrl)
  val redirectToService: play.api.mvc.Result = Redirect(appConfig.stcServiceUrl)

}