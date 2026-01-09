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

package connectors

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.EnrolmentResponse.EnrolmentSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.SubscriptionResponse.SubscriptionSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.{IndividualEnrolmentDetails, IndividualSubscriptionDetails, RegistrationClient}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{AlfAddress, AlfConfirmedAddress, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.AddressPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.{WhatsYourContactNumberPage, WhatsYourEmailAddressPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationDataNotFoundException(msg: String) extends RuntimeException(msg)
class SubscriptionErrorException(msg: String) extends RuntimeException(msg)
class EnrolmentErrorException(msg: String) extends RuntimeException(msg)

trait SubscriptionConnector:
  def subscribeAndEnrolIndividual(userId: String)(nino: String)(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit]
  
class SubscriptionConnectorImpl @Inject() (registrationClient: RegistrationClient,
                                            registrationDataRepository: RegistrationDataRepository)
                                          (implicit ec: ExecutionContext) extends SubscriptionConnector with Logging:


  override def subscribeAndEnrolIndividual(userId: String)(nino: String)(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      maybeSafeId    <- registrationDataRepository.getRegistrationData(userId).map(_.safeId)
      subscriptionId <- subscribe(maybeSafeId, userAnswers)
      _              <- enrol(subscriptionId, nino)
    } yield ()
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


  private val buildSubscriptionDetails: String => UserAnswers => Option[IndividualSubscriptionDetails] = { safeId =>
    answers =>
      for {
        alf          <- getAddress(answers)
        address       = alf.address
        (l1, l2, l3) <- extractLines(address)
        email        <- getEmailAddress(answers)
        tel          <- getTelephoneNumber(answers)
      } yield {
        IndividualSubscriptionDetails(safeId, l1, l2, l3, address.postcode, address.country.code, tel, None, email)
      }
  }

  private val getAddress: UserAnswers => Option[AlfConfirmedAddress] = _.get[AlfConfirmedAddress](AddressPage())
  private val getEmailAddress: UserAnswers => Option[String] = _.get[String](WhatsYourEmailAddressPage)
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


