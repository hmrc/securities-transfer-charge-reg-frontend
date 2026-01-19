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

package uk.gov.hmrc.securitiestransferchargeregfrontend.audit

import play.api.libs.json.Writes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.securitiestransferchargeregfrontend.audit.RegistrationAuditModel
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualData

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RegistrationAuditService @Inject()(
                                          auditConnector: AuditConnector
                                        )(implicit ec: ExecutionContext) {

  private val auditType = "RegistrationSubmission"

  private def sendAuditEvent[T](
                                 event: RegistrationAuditModel[T]
                               )(implicit hc: HeaderCarrier, writes: Writes[RegistrationAuditModel[T]]): Unit = {
    auditConnector.sendExplicitAudit(
      auditType = auditType,
      detail = event
    )
  }


  def auditOrganisationRegistrationComplete(
                                             registrationStarted: Option[Instant],
                                             userAnswers: UserAnswers,
                                             credId: String
                                           )(implicit hc: HeaderCarrier): Unit =
    buildOrganisationAuditModel(registrationStarted, userAnswers, credId).foreach(sendAuditEvent)

  def auditIndividualRegistrationComplete(
                                           registrationStarted: Option[Instant],
                                           data: ValidIndividualData,
                                           userAnswers: UserAnswers
                                         )(implicit hc: HeaderCarrier): Unit =
    buildIndividualAuditModel(registrationStarted, data, userAnswers).foreach(sendAuditEvent)

  private def buildOrganisationAuditModel(
                                           registrationStarted: Option[Instant],
                                           userAnswers: UserAnswers,
                                           credId: String
                                         ): Option[RegistrationAuditModel[OrganisationDetailsPayload]] =
    for {
      details <- RegistrationDetailsPayload.fromOrganisation(userAnswers)
    } yield RegistrationAuditModel(
      userEnteredDetails = details,
      affinityGroup = "Organisation",
      registrationStarted = registrationStarted,
      credentialId = credId
    )

  private def buildIndividualAuditModel(registrationStarted: Option[Instant],
                                        data: ValidIndividualData,
                                        userAnswers: UserAnswers
                                       ): Option[RegistrationAuditModel[IndividualDetailsPayload]] =
    for {
      details <- RegistrationDetailsPayload.fromIndividual(data, userAnswers)
    } yield RegistrationAuditModel(
      userEnteredDetails = details,
      affinityGroup = "Individual",
      registrationStarted = registrationStarted,
      credentialId = data.credId
    )

}
