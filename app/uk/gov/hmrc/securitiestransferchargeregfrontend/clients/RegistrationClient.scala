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
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.EnrolmentResponse.EnrolmentSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.IndividualRegistrationDetails.format
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionResponse.SubscriptionSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionStatus.SubscriptionActive
import uk.gov.hmrc.securitiestransferchargeregfrontend.config.FrontendAppConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait RegistrationClient:
  def hasCurrentSubscription(etmpSafeId: String): Future[SubscriptionStatusResult]
  def register(individualRegistrationDetails: IndividualRegistrationDetails)(implicit hc: HeaderCarrier): Future[RegistrationResult]
  def subscribe(individualSubscriptionDetails: IndividualSubscriptionDetails)(implicit hc: HeaderCarrier): Future[SubscriptionResult]
  def subscribe(organisationSubscriptionDetails: OrganisationSubscriptionDetails): Future[SubscriptionResult]
  def enrolIndividual(enrolmentDetails: IndividualEnrolmentDetails): Future[EnrolmentResult]

// DUMMY IMPL until we have a real BE implementation for this.
class RegistrationClientImpl @Inject()(
                                        http: HttpClientV2,
                                        config: FrontendAppConfig
                                      )(implicit ec: ExecutionContext) extends RegistrationClient with Logging {
  override def hasCurrentSubscription(etmpSafeId: String): Future[SubscriptionStatusResult] = Future.successful(Right(SubscriptionActive))
  override def register(
                         individualRegistrationDetails: IndividualRegistrationDetails
                       )(implicit hc: HeaderCarrier): Future[RegistrationResult] = {

    val url = url"${config.registerIndividualBackendUrl}"

    http
      .post(url)
      .withBody(Json.toJson(individualRegistrationDetails): JsValue)
      .execute[HttpResponse]
      .map { resp =>
        resp.status match {

          case OK =>
            Json.parse(resp.body).validate[RegisterIndividualResponseDto].asEither match {
              case Right(dto) =>
                Right(RegistrationResponse.RegistrationSuccessful(dto.safeId))

              case Left(errors) =>
                val msg =
                  s"RegistrationClient.register: Could not parse OK response. errors=$errors body=${resp.body}"
                logger.warn(msg)
                Left(RegistrationClientError(msg))
            }

          case BAD_REQUEST =>
            Left(RegistrationClientError(s"RegistrationClient.register: 400 from BE. body=${resp.body}"))

          case status =>
            Left(RegistrationServerError(s"RegistrationClient.register: unexpected status=$status body=${resp.body}"))
        }
      }
      .recover {
        case NonFatal(e) =>
          val msg = s"RegistrationClient.register: exception calling BE: ${e.getMessage}"
          logger.error(msg, e)
          Left(RegistrationServerError(msg))
      }
  }

  override def subscribe(details: IndividualSubscriptionDetails)(implicit hc: HeaderCarrier): Future[SubscriptionResult] = {
    val url = url"${config.subscribeIndividualBackendUrl}"

    http.post(url)
      .withBody(Json.toJson(details): JsValue)
      .execute[HttpResponse]
      .map { resp =>
        resp.status match {
          case OK =>
            Json.parse(resp.body).validate[IndividualSubscriptionResponseDto].asEither match {
              case Right(dto) => Right(SubscriptionResponse.SubscriptionSuccessful(dto.subscriptionId))
              case Left(errs) => Left(SubscriptionClientError(s"Could not parse OK response. errs=$errs body=${resp.body}"))
            }
          case BAD_REQUEST =>
            Left(SubscriptionClientError(s"400 from BE. body=${resp.body}"))
          case status =>
            Left(SubscriptionServerError(s"unexpected status=$status body=${resp.body}"))
        }
      }
      .recover { case NonFatal(e) =>
        Left(SubscriptionServerError(s"exception calling BE: ${e.getMessage}"))
      }
  }
  override def subscribe(organisationSubscriptionDetails: OrganisationSubscriptionDetails): Future[SubscriptionResult] = Future.successful(Right(SubscriptionSuccessful("SUBSCRIPTION123")))
  override def enrolIndividual(enrolmentDetails: IndividualEnrolmentDetails): Future[EnrolmentResult] = Future.successful(Right(EnrolmentSuccessful))

}