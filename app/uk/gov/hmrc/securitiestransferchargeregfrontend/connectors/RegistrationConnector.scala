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
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualData
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.CommonHelpers
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.{IndividualRegistrationDetails, RegistrationClient, RegistrationResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


trait RegistrationConnector:
  def clearRegistration(userId: String): Future[Unit]
  def registerIndividual(userId: String)(data: ValidIndividualData)(dateOfBirth: String)(implicit hc: HeaderCarrier): Future[Unit]

class RegistrationErrorException(msg: String) extends RuntimeException(msg)

class RegistrationConnectorImpl @Inject() ( registrationClient: RegistrationClient,
                                            registrationDataRepository: RegistrationDataRepository)
                                          ( implicit ec: ExecutionContext) extends RegistrationConnector with Logging:

  private val logInfoAndFail = CommonHelpers.logInfoAndFail(logger)
  
  private def buildIndividualRequest(data: ValidIndividualData, dateOfBirth: String): IndividualRegistrationDetails = {
    IndividualRegistrationDetails(
      firstName = data.firstName,
      middleName = None,
      lastName = data.lastName,
      dateOfBirth = dateOfBirth,
      nino = data.nino
    )
  }

  override def clearRegistration(userId: String): Future[Unit] = {
    logger.info(s"Removing registration data for user [$userId]")
    registrationDataRepository.setSafeId(userId)(None)
  }
  
  override def registerIndividual(userId: String)(data: ValidIndividualData)(dateOfBirth: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    registrationClient.register(buildIndividualRequest(data, dateOfBirth)).flatMap {
      case Right(RegistrationResponse.RegistrationSuccessful(safeId)) =>
        registrationDataRepository.setSafeId(userId)(Some(safeId)).map(_ =>
          logger.info(s"Added registration data for user [$userId]")
        )
      case Right(_) =>
        val ex = new RegistrationErrorException(s"RegistrationConnector: Unsuccessful response when registering userId: $userId")
        logInfoAndFail(ex)
      case Left(error) =>
        val ex = new RegistrationErrorException(s"RegistrationConnector: Error response when registering userId: $userId, error: $error")
        logInfoAndFail(ex)
    }
  }
