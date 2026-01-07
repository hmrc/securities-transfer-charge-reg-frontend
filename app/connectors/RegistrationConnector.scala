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
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualData
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.RegistrationDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


trait RegistrationConnector {
  def registerIndividual(userId: String)(data: ValidIndividualData)(dateOfBirth: String)(implicit hc: HeaderCarrier): Future[Unit]
}

class RegistrationErrorException(msg: String) extends RuntimeException(msg)

class RegistrationConnectorImpl @Inject() ( registrationClient: RegistrationClient,
                                            registrationDataRepository: RegistrationDataRepository)
                                          (implicit ec: ExecutionContext) extends RegistrationConnector with Logging:

  private def buildIndividualRequest(data: ValidIndividualData, dateOfBirth: String): IndividualRegistrationDetails = {
    IndividualRegistrationDetails(
      firstName = data.firstName,
      middleName = None,
      lastName = data.lastName,
      dateOfBirth = dateOfBirth,
      nino = data.nino
    )
  }

  override def registerIndividual(userId: String)(data: ValidIndividualData)(dateOfBirth: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    registrationClient.register(buildIndividualRequest(data, dateOfBirth)).flatMap {
      case Right(RegistrationResponse.RegistrationSuccessful(safeId)) =>
        registrationDataRepository.setSafeId(userId)(safeId).map(_ => ())
      case Right(_) =>
        val msg = s"RegistrationConnector: Unsuccessful response when registering userId: $userId"
        logger.info(msg)
        Future.failed(new RegistrationErrorException(msg))
      case Left(error) =>
        val msg = s"RegistrationConnector: Error response when registering userId: $userId, error: $error"
        logger.info(msg)
        Future.failed(new RegistrationErrorException(msg))
    }
  }
