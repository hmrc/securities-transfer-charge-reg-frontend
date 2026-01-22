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

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.audit.RegistrationAuditService
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.EnrolmentResponse.EnrolmentSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.SubscriptionResponse.SubscriptionSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualData
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.CommonHelpers
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.CommonHelpers.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationDataNotFoundException(msg: String) extends RuntimeException(msg)
class SubscriptionErrorException(msg: String) extends RuntimeException(msg)
class EnrolmentErrorException(msg: String) extends RuntimeException(msg)

trait SubscriptionConnector:
  def subscribeAndEnrolIndividual(userId: String)(userAnswers: UserAnswers, data: ValidIndividualData)(implicit hc: HeaderCarrier): Future[Unit]
  def subscribeAndEnrolOrganisation(userId: String, credId: String)(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit]

class SubscriptionConnectorImpl @Inject()(registrationClient: RegistrationClient,
                                          registrationDataRepository: RegistrationDataRepository,
                                          registrationAuditService: RegistrationAuditService)
                                         (implicit ec: ExecutionContext) extends SubscriptionConnector with Logging:


  override def subscribeAndEnrolIndividual(userId: String)(userAnswers: UserAnswers, data: ValidIndividualData)(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      regData         <- registrationDataRepository.getRegistrationData(userId)
      subscriptionId  <- subscribe(regData.safeId, userAnswers)
      _               <- enrol(subscriptionId, data.nino)
    } yield registrationAuditService.auditIndividualRegistrationComplete(regData.startedAt,data,userAnswers)
  }

  override def subscribeAndEnrolOrganisation(userId: String, credId: String)(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      regData         <- registrationDataRepository.getRegistrationData(userId)
      subscriptionId  <- subscribeOrganisation(regData.safeId, userAnswers)
      _               <- enrolOrganisation(subscriptionId, regData.ctUtr)
    } yield registrationAuditService.auditOrganisationRegistrationComplete(regData.startedAt, userAnswers, credId)
  }
  
  private val logInfoAndFail = CommonHelpers.logInfoAndFail(logger)
  
  private def subscribe(maybeSafeId: Option[String], userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[String] = {
    val maybeResult = for {
      safeId  <- maybeSafeId
      details <- buildSubscriptionDetails(safeId)(userAnswers)
    } yield {
      registrationClient
        .subscribe(details)
        .flatMap(subscriptionResultHandler(userAnswers.id))
    }
    maybeResult.getOrElse {
      logInfoAndFail(new RegistrationDataNotFoundException("Subscription failed: missing safeId or subscription details"))
    }
  }

  private def subscribeOrganisation(
    maybeSafeId: Option[String],
    userAnswers: UserAnswers)(
    implicit hc: HeaderCarrier): Future[String] = {

    val maybeResult = for {
      safeId  <- maybeSafeId
      details <- buildOrganisationSubscriptionDetails(safeId)(userAnswers)
    }
    yield {
      registrationClient
        .subscribe(details)
        .flatMap(subscriptionResultHandler(userAnswers.id))
    }
    maybeResult.getOrElse {
      Future.failed(new RegistrationDataNotFoundException("Subscription failed: missing safeId or subscription details"))
    }
  }

  private def subscriptionResultHandler(id: String)(subscriptionResult: SubscriptionResult): Future[String] = subscriptionResult match {

    case Right(SubscriptionSuccessful(subscriptionId)) =>
      registrationDataRepository.setSubscriptionId(id)(subscriptionId).map(_ => subscriptionId)

    case Right(_) =>
      val msg = s"SubscriptionConnector: Unsuccessful response when subscribing userId: $id"
      logInfoAndFail(new SubscriptionErrorException(msg))

    case Left(error) =>
      val msg = s"SubscriptionConnector: Error response when subscribing userId: $id, error: $error"
      logInfoAndFail(new SubscriptionErrorException(msg))
  }

  type SubscriptionData = Option[(String, String, Option[String], Option[String], String, String, String, Option[String], String)]
  private val buildSubscriptionData: String => UserAnswers => SubscriptionData = { safeId => answers =>
    for {
      alf           <- getAddress(answers)
      address       =  alf.address
      (l1, l2, l3)  <- extractLines(address)
      email         <- getEmailAddress(answers)
      tel           <- getTelephoneNumber(answers)
    } yield (safeId, l1, l2, l3, address.postcode, address.country.code, tel, None, email)
  }
  
  private val   buildSubscriptionDetails: String => UserAnswers => Option[IndividualSubscriptionDetails] = { safeId => answers =>
    buildSubscriptionData(safeId)(answers).map(IndividualSubscriptionDetails.apply.tupled)
  }
  
  private val buildOrganisationSubscriptionDetails: String => UserAnswers => Option[OrganisationSubscriptionDetails] = { safeId => answers =>
    buildSubscriptionData(safeId)(answers).map(OrganisationSubscriptionDetails.apply.tupled)
  }

  private def enrol( subscriptionId: String,
                     nino: String)
                   ( implicit hc: HeaderCarrier): Future[Unit] = {
    registrationClient
      .enrolIndividual(IndividualEnrolmentDetails(subscriptionId, nino))
      .flatMap(enrolResultHandler(subscriptionId))
  }

  private def enrolOrganisation( subscriptionId: String,
                                 maybeUtr: Option[String])
                               ( implicit hc: HeaderCarrier): Future[Unit] = {

    maybeUtr.map { ctUtr =>
      registrationClient
        .enrolOrganisation(OrganisationEnrolmentDetails(subscriptionId, ctUtr))
        .flatMap(enrolResultHandler(subscriptionId))
    }.getOrElse {
      logInfoAndFail(
        new EnrolmentErrorException(s"SubscriptionConnector: Missing UTR when enrolling subscriptionId: $subscriptionId")
      )
    }
  }

  private def enrolResultHandler(subscriptionId: String)(enrolmentResult: EnrolmentResult): Future[Unit] = enrolmentResult match {
    case Right(EnrolmentSuccessful) =>
      Future.successful(())

    case Right(_) =>
      val msg = s"SubscriptionConnector: Unsuccessful response when enrolling subscriptionId: $subscriptionId"
      logInfoAndFail(new EnrolmentErrorException(msg))

    case Left(error) =>
      val msg = s"SubscriptionConnector: Error response when enrolling subscriptionId: $subscriptionId, error: $error"
      logInfoAndFail(new EnrolmentErrorException(msg))
  }
  
