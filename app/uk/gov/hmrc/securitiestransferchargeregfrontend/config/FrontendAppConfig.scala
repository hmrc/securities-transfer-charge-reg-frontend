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

package uk.gov.hmrc.securitiestransferchargeregfrontend.config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  val stcEnrolmentKey = "HMRC-STC-ORG"

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "securities-transfer-charge-reg-frontend"

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val loginUrl: String         = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String       = configuration.get[String]("urls.signOut")

  val unauthorisedUrl: String = configuration.get[String]("microservice.redirects.unauthorised-url")
  val registerUrl: String = configuration.get[String]("microservice.redirects.register-url")
  val registerIndividualUrl: String = configuration.get[String]("microservice.redirects.register-individual-url")
  val asaUrl: String = configuration.get[String]("microservice.redirects.asa-url")
  val registerOrganisationUrl: String = configuration.get[String]("microservice.redirects.register-organisation-url")
  val ivUpliftUrl: String = configuration.get[String]("microservice.redirects.iv-uplift-url")
  val stcServiceUrl: String = configuration.get[String]("microservice.redirects.stc-service-url")

  private val addressLookupBaseUrl: String =
    servicesConfig.baseUrl("address-lookup-frontend")

  val alfInitUrl: String =
    s"$addressLookupBaseUrl/api/init"

  val alfRetrieveUrl: String =
    s"$addressLookupBaseUrl/api/confirmed"

  val alfIndividualsContinueUrl: String =
    s"$host/register-securities-transfer-charge/address/return"

  val alfOrgContinueUrl: String =
    s"$host/register-securities-transfer-charge/org/address/return"

  private val registrationBackendBaseUrl: String =
    servicesConfig.baseUrl("securities-transfer-charge-registration")

  val registerIndividualBackendUrl: String =
    s"$registrationBackendBaseUrl/securities-transfer-charge-registration/registration/individual"

  val subscribeIndividualBackendUrl: String =
    s"$registrationBackendBaseUrl/securities-transfer-charge-registration/subscription/individual"

  val enrolIndividualBackendUrl: String =
    s"$registrationBackendBaseUrl/securities-transfer-charge-registration/enrolment/individual"

  val hasCurrentSubscriptionBaseUrl: String =
    s"$registrationBackendBaseUrl/securities-transfer-charge-registration/subscription"


  private val exitSurveyBaseUrl: String = configuration.get[Service]("microservice.services.feedback-frontend").baseUrl
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/securities-transfer-charge-reg-frontend"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Int]("mongodb.timeToLiveInSeconds")
  
  val taxAgentLink: String = configuration.get[String]("urls.taxAgentLink")
  val hmrcOnlineServicesLink: String = configuration.get[String]("urls.hmrcOnlineServicesLink")
}
