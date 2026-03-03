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
import uk.gov.hmrc.securitiestransferchargeregfrontend.audit.{RegistrationAuditService, RegistrationOutcome}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.EnrolmentResponse.EnrolmentSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.SubscriptionResponse.SubscriptionSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualData
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.{RegistrationData, RegistrationDataRepository}
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.CommonHelpers
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.CommonHelpers.*

import java.time.Instant
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

  private type AuditFailureHandler[A] = Option[Instant] => PartialFunction[Throwable, Future[A]]
  private val logInfoAndFail = CommonHelpers.logInfoAndFail(logger)
  
  private def noSafeId[A]: Future[A]  = logInfoAndFail(new RegistrationDataNotFoundException("Enrolment failed: missing safeId"))
  private def noDetails[A]: Future[A] = logInfoAndFail(new RegistrationDataNotFoundException("Subscription failed: missing subscription details"))
  private def noUtr[A] : Future[A]    = logInfoAndFail(new RegistrationDataNotFoundException("Enrolment failed: missing ctUtr"))

  override def subscribeAndEnrolIndividual(userId: String)(userAnswers: UserAnswers, data: ValidIndividualData)(implicit hc: HeaderCarrier): Future[Unit] = {
    val auditFailedSub: AuditFailureHandler[String] = s => t => auditSubscriptionFailure(s, userAnswers, data, t)
    val auditFailedEnrol: AuditFailureHandler[Unit] = s => t => auditEnrolmentFailure(s, userAnswers, data, t)
    for {
      regData         <- registrationDataRepository.getRegistrationData(userId)
      startedAt        = regData.startedAt
      safeId          <- getSafeId(regData)
      subscriptionId  <- subscribe(safeId, userAnswers, data).recoverWith(auditFailedSub(startedAt))
      _               <- enrol(subscriptionId, data.nino).recoverWith(auditFailedEnrol(startedAt))
    } yield registrationAuditService.auditIndividualRegistrationComplete(startedAt, data, userAnswers)
  }

  private def auditSubscriptionFailure(registrationStarted: Option[Instant], userAnswers: UserAnswers, data: ValidIndividualData, t: Throwable)(implicit hc: HeaderCarrier): Future[String] = {
    registrationAuditService.auditIndividualRegistrationComplete(registrationStarted, data, userAnswers, RegistrationOutcome.subscriptionFailed(t.getLocalizedMessage))
    Future.failed(t)
  }

  private def auditEnrolmentFailure(registrationStarted: Option[Instant], userAnswers: UserAnswers, data: ValidIndividualData, t: Throwable)(implicit hc: HeaderCarrier): Future[Unit] = {
    registrationAuditService.auditIndividualRegistrationComplete(registrationStarted, data, userAnswers, RegistrationOutcome.enrolmentFailed(t.getLocalizedMessage))
    Future.failed(t)
  }

  override def subscribeAndEnrolOrganisation(userId: String, credId: String)(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit] = {
    val auditFailedSub: AuditFailureHandler[String] = s => t => auditOrgSubscriptionFailure(s, userAnswers, credId, t)
    val auditFailedEnrol: AuditFailureHandler[Unit] = s => t => auditOrgEnrolmentFailure(s, userAnswers, credId, t)
    for {
      regData         <- registrationDataRepository.getRegistrationData(userId)
      startedAt        = regData.startedAt
      safeId          <- getSafeId(regData)
      utr             <- getUtr(regData)
      subscriptionId  <- subscribeOrganisation(safeId, userAnswers).recoverWith(auditFailedSub(startedAt))
      _               <- enrolOrganisation(subscriptionId, utr).recoverWith(auditFailedEnrol(startedAt))
    } yield registrationAuditService.auditOrganisationRegistrationComplete(startedAt, userAnswers, credId)
  }

  private def auditOrgSubscriptionFailure(registrationStarted: Option[Instant], userAnswers: UserAnswers, credId: String, t: Throwable)(implicit hc: HeaderCarrier): Future[String] = {
    registrationAuditService.auditOrganisationRegistrationComplete(registrationStarted, userAnswers, credId, RegistrationOutcome.subscriptionFailed(t.getLocalizedMessage))
    Future.failed(t)
  }

  private def auditOrgEnrolmentFailure(registrationStarted: Option[Instant], userAnswers: UserAnswers, credId: String, t: Throwable)(implicit hc: HeaderCarrier): Future[Unit] = {
    registrationAuditService.auditOrganisationRegistrationComplete(registrationStarted, userAnswers, credId, RegistrationOutcome.enrolmentFailed(t.getLocalizedMessage))
    Future.failed(t)
  }

  private val getSafeId: RegistrationData => Future[String] = data =>
    data.safeId.fold(noSafeId)(Future.successful)

  private val getUtr: RegistrationData => Future[String] = data =>
    data.ctUtr.fold(noUtr)(Future.successful)

  private def subscribe(safeId: String, userAnswers: UserAnswers, data: ValidIndividualData)(implicit hc: HeaderCarrier): Future[String] = {
    buildSubscriptionDetails(safeId)(userAnswers)(data)
      .fold(noDetails)(registrationClient
        .subscribe(_)
        .flatMap(subscriptionResultHandler(userAnswers.id)))
  }

  private def subscribeOrganisation(safeId: String,
                                    userAnswers: UserAnswers)
                                   (implicit hc: HeaderCarrier): Future[String] = {
    buildOrganisationSubscriptionDetails(safeId)(userAnswers)
      .fold(noDetails)(registrationClient
        .subscribe(_)
        .flatMap(subscriptionResultHandler(userAnswers.id)))
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

  private def normalizeWhitespace(s: String): String =
    s.replaceAll("\\s+", " ").trim

  private def buildContactName(data: ValidIndividualData): String =
    normalizeWhitespace(s"${data.firstName} ${data.lastName}")

  private val buildSubscriptionDetails: String => UserAnswers => ValidIndividualData => Option[IndividualSubscriptionDetails] = { safeId =>
    answers =>
      data =>
        for {
          alf <- getIndividualAddress(answers)
          address = alf.address
          (l1, l2, l3) <- extractLines(address)
          email <- getEmailAddress(answers)
          tel <- getTelephoneNumber(answers)
        } yield IndividualSubscriptionDetails(safeId, buildContactName(data), l1, l2, l3, address.postcode, address.country.code, tel, None, email)
  }

  private val buildOrganisationSubscriptionDetails: String => UserAnswers => Option[OrganisationSubscriptionDetails] = { safeId => answers =>
    for {
      alf          <- getOrgAddress(answers)
      address      =  alf.address
      (l1, l2, l3) <- extractLines(address)
      email        <- getContactEmailAddress(answers)
      tel          <- getContactNumber(answers)
    } yield OrganisationSubscriptionDetails(safeId, l1, l2, l3, address.postcode, address.country.code, tel, None, email)
  }

  private def enrol( subscriptionId: String,
                     nino: String)
                   ( implicit hc: HeaderCarrier): Future[Unit] = {
    registrationClient
      .enrolIndividual(IndividualEnrolmentDetails(subscriptionId, nino))
      .flatMap(enrolResultHandler(subscriptionId))
  }

  private def enrolOrganisation( subscriptionId: String,
                                 utr: String)
                               ( implicit hc: HeaderCarrier): Future[Unit] = {

    registrationClient
      .enrolOrganisation(OrganisationEnrolmentDetails(subscriptionId, utr))
      .flatMap(enrolResultHandler(subscriptionId))
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
