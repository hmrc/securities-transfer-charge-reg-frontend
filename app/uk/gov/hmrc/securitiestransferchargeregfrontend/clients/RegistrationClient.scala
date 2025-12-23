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

import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.EnrolmentResponse.EnrolmentSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionResponse.SubscriptionSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionStatus.SubscriptionActive
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait RegistrationClient:
  def hasCurrentSubscription(etmpSafeId: String): Future[SubscriptionStatusResult]
  def register(individualRegistrationDetails: IndividualRegistrationDetails)(implicit hc: HeaderCarrier): Future[RegistrationResponse]
  def subscribe(individualSubscriptionDetails: IndividualSubscriptionDetails): Future[SubscriptionResult]
  def subscribe(organisationSubscriptionDetails: OrganisationSubscriptionDetails): Future[SubscriptionResult]
  def enrolIndividual(enrolmentDetails: IndividualEnrolmentDetails): Future[EnrolmentResult]

class RegistrationClientImpl @Inject()(
                                        http: HttpClientV2,
                                        config: FrontendAppConfig
                                      )(implicit ec: ExecutionContext)
  extends RegistrationClient {

  override def hasCurrentSubscription(etmpSafeId: String): Future[SubscriptionStatusResult] = Future.successful(Right(SubscriptionActive))

  override def register(
                         details: IndividualRegistrationDetails
                       )(implicit hc: HeaderCarrier): Future[RegistrationResponse] =
    http
      .post(url"${config.registerIndividualBackendUrl}")
      .withBody(Json.toJson(details))
      .execute[HttpResponse]
      .map {
        case res if res.status == OK =>
          RegistrationSuccessful
        case _ =>
          RegistrationFailed
      }

  override def subscribe(individualSubscriptionDetails: IndividualSubscriptionDetails): Future[SubscriptionResult] = Future.successful(Right(SubscriptionSuccessful))
  override def subscribe(organisationSubscriptionDetails: OrganisationSubscriptionDetails): Future[SubscriptionResult] = Future.successful(Right(SubscriptionSuccessful))
  override def enrolIndividual(enrolmentDetails: IndividualEnrolmentDetails): Future[EnrolmentResult] = Future.successful(Right(EnrolmentSuccessful))

}