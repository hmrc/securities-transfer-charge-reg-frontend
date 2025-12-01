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

package uk.gov.hmrc.securitiestransferchargeregfrontend.clients

import play.api.Logging
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.RegistrationResponse.RegistrationSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionResponse.SubscriptionSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionStatus.SubscriptionActive

import javax.inject.Inject

trait RegistrationClient:
  def hasCurrentSubscription: SubscriptionStatusResult
  def register(individualRegistrationDetails: IndividualRegistrationDetails): RegistrationResult
  def subscribe(individualSubscriptionDetails: IndividualSubscriptionDetails): SubscriptionResult
  def subscribe(organisationSubscriptionDetails: OrganisationSubscriptionDetails): SubscriptionResult

class RegistrationClientImpl @Inject() /*(appConf: FrontendAppConfig, http: HttpClientV2)(implicit ec: ExecutionContext) */extends RegistrationClient with Logging {
  override def hasCurrentSubscription: SubscriptionStatusResult = Right(SubscriptionActive)
  override def register(individualRegistrationDetails: IndividualRegistrationDetails): RegistrationResult = Right(RegistrationSuccessful)
  override def subscribe(individualSubscriptionDetails: IndividualSubscriptionDetails): SubscriptionResult = Right(SubscriptionSuccessful)
  override def subscribe(organisationSubscriptionDetails: OrganisationSubscriptionDetails): SubscriptionResult = Right(SubscriptionSuccessful)
}