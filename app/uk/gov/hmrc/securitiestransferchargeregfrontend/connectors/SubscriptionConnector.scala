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
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.SubscriptionResponse.{SubscriptionFailed, SubscriptionSuccessful}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualData
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{AlfAddress, AlfConfirmedAddress, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.AddressPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.{WhatsYourContactNumberPage, WhatsYourEmailAddressPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.{ContactEmailAddressPage, ContactNumberPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationDataNotFoundException(msg: String) extends RuntimeException(msg)

class SubscriptionErrorException(msg: String) extends RuntimeException(msg)

class EnrolmentErrorException(msg: String) extends RuntimeException(msg)

trait SubscriptionConnector:
  def subscribeAndEnrolIndividual(userId: String)(nino: String)(userAnswers: UserAnswers)(data: ValidIndividualData)(implicit hc: HeaderCarrier): Future[Unit]

  def subscribeAndEnrolOrganisation(userId: String)(userAnswers: UserAnswers)(credId: String)(implicit hc: HeaderCarrier): Future[Unit]

class SubscriptionConnectorImpl @Inject()(registrationClient: RegistrationClient,
                                          registrationDataRepository: RegistrationDataRepository, registrationAuditService: RegistrationAuditService,
                                         )
                                         (implicit ec: ExecutionContext) extends SubscriptionConnector with Logging:


  override def subscribeAndEnrolIndividual(userId: String)(nino: String)(userAnswers: UserAnswers)(data: ValidIndividualData)(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      registration <- registrationDataRepository.getRegistrationData(userId)
      subscriptionId <- subscribe(registration.safeId, userAnswers)
      _ <- enrol(subscriptionId, nino)
    } yield registrationAuditService.auditIndividualRegistrationComplete(registration.startedAt,data,userAnswers)
  }

  override def subscribeAndEnrolOrganisation(userId: String)(userAnswers: UserAnswers)(credId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      safeId <- Future.successful(Some("SAFE123")) // Dummy value to simulate value returned from GRS
      ctUtr <- Future.successful("0123456789") // Dummy value to simulate value returned from GRS
      subscriptionId <- subscribeOrganisation(safeId, userAnswers)
      _ <- enrolOrganisation(subscriptionId, ctUtr)
      startedAt <- registrationDataRepository.getRegistrationData(userId).map(_.startedAt)
    } yield registrationAuditService.auditOrganisationRegistrationComplete(startedAt,userAnswers,credId)
  }

  private def subscribe(maybeSafeId: Option[String], userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[String] =
    maybeSafeId match {
      case None =>
        Future.failed(new RegistrationDataNotFoundException("Subscription failed: missing safeId"))

      case Some(safeId) =>
        buildSubscriptionDetails(safeId)(userAnswers) match {
          case None =>
            Future.failed(new RegistrationDataNotFoundException("Subscription failed: missing subscription details"))

          case Some(details) =>
            registrationClient.subscribe(details).flatMap {
              case Right(SubscriptionSuccessful(subscriptionId)) =>
                registrationDataRepository.setSubscriptionId(userAnswers.id)(subscriptionId).map(_ => subscriptionId)

              case Right(_) =>
                val msg = s"SubscriptionConnector: Unsuccessful response when subscribing userId: ${userAnswers.id}"
                logger.info(msg)
                Future.failed(new SubscriptionErrorException(msg))

              case Left(error) =>
                val msg = s"SubscriptionConnector: Error response when subscribing userId: ${userAnswers.id}, error: $error"
                logger.info(msg)
                Future.failed(new SubscriptionErrorException(msg))
            }
        }
    }

  private def subscribeOrganisation(
                                     maybeSafeId: Option[String],
                                     userAnswers: UserAnswers
                                   )(implicit hc: HeaderCarrier): Future[String] = {

    for {
      safeId <- maybeSafeId.fold[Future[String]](Future.failed(new RegistrationDataNotFoundException("Subscription failed: missing safeId"))
      )(Future.successful)

      details <- buildOrganisationSubscriptionDetails(safeId, userAnswers).fold[Future[OrganisationSubscriptionDetails]](
        Future.failed(new RegistrationDataNotFoundException("Subscription failed: missing subscription details"))
      )(Future.successful)

      subscriptionResult <- registrationClient.subscribe(details)

      subscriptionId <- subscriptionResult match {
        case Right(SubscriptionSuccessful(id)) => registrationDataRepository.setSubscriptionId(userAnswers.id)(id).map(_ => id)

        case Right(SubscriptionFailed) =>
          val msg = s"SubscriptionConnector: Unsuccessful response when subscribing userId: ${userAnswers.id}"
          logger.info(msg)
          Future.failed(new SubscriptionErrorException(msg))

        case Left(error) =>
          val msg = s"SubscriptionConnector: Error response when subscribing userId: ${userAnswers.id}, error: $error"
          logger.info(msg)
          Future.failed(new SubscriptionErrorException(msg))
      }
    } yield subscriptionId
  }


  private val buildSubscriptionDetails: String => UserAnswers => Option[IndividualSubscriptionDetails] = { safeId =>
    answers =>
      for {
        alf <- getAddress(answers)
        address = alf.address
        (l1, l2, l3) <- extractLines(address)
        email <- getEmailAddress(answers)
        tel <- getTelephoneNumber(answers)
      } yield {
        IndividualSubscriptionDetails(safeId, l1, l2, l3, address.postcode, address.country.code, tel, None, email)
      }
  }

  private def buildOrganisationSubscriptionDetails(safeId: String, userAnswers: UserAnswers): Option[OrganisationSubscriptionDetails] = {
    for {
      alf <- getAddress(userAnswers)
      address = alf.address
      (l1, l2, l3) <- extractLines(address)
      email <- getContactEmailAddress(userAnswers)
      tel <- getContactNumber(userAnswers)
    } yield OrganisationSubscriptionDetails(safeId, l1, l2, l3, address.postcode, address.country.code, tel, None, email)
  }

  private val getAddress: UserAnswers => Option[AlfConfirmedAddress] = _.get[AlfConfirmedAddress](AddressPage())
  private val getEmailAddress: UserAnswers => Option[String] = _.get[String](WhatsYourEmailAddressPage)
  private val getContactEmailAddress: UserAnswers => Option[String] = _.get[String](ContactEmailAddressPage)
  private val getContactNumber: UserAnswers => Option[String] = _.get[String](ContactNumberPage)
  private val getTelephoneNumber: UserAnswers => Option[String] = _.get[String](WhatsYourContactNumberPage)

  private val extractLines: AlfAddress => Option[(String, Option[String], Option[String])] = { address =>
    val lines = address.lines
    lines.headOption.map { h =>
      (h, lines.lift(1), lines.lift(2))
    }
  }

  private def enrol(
                     subscriptionId: String,
                     nino: String
                   )(implicit hc: HeaderCarrier): Future[Unit] = {

    registrationClient.enrolIndividual(
      IndividualEnrolmentDetails(subscriptionId, nino)
    ).flatMap {
      case Right(EnrolmentSuccessful) =>
        Future.successful(())

      case Right(_) =>
        val msg =
          s"SubscriptionConnector: Unsuccessful response when enrolling subscriptionId: $subscriptionId"
        logger.info(msg)
        Future.failed(new EnrolmentErrorException(msg))

      case Left(error) =>
        val msg =
          s"SubscriptionConnector: Error response when enrolling subscriptionId: $subscriptionId, error: $error"
        logger.info(msg)
        Future.failed(new EnrolmentErrorException(msg))
    }
  }

  private def enrolOrganisation(
                                 subscriptionId: String,
                                 ctUtr: String
                               )(implicit hc: HeaderCarrier): Future[Unit] = {

    registrationClient
      .enrolOrganisation(OrganisationEnrolmentDetails(subscriptionId, ctUtr))
      .map {
        case Right(EnrolmentSuccessful) => ()
        case Right(_) =>
          val msg =
            s"SubscriptionConnector: Unsuccessful response when enrolling subscriptionId: $subscriptionId"
          logger.info(msg)
          throw new EnrolmentErrorException(msg)
        case Left(error) =>
          val msg =
            s"SubscriptionConnector: Error response when enrolling subscriptionId: $subscriptionId, error: $error"
          logger.info(msg)
          throw new EnrolmentErrorException(msg)
      }
  }



