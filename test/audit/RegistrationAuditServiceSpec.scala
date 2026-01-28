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

package audit

import base.{Fixtures, SpecBase}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, times, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.securitiestransferchargeregfrontend.audit.{IndividualDetailsPayload, OrganisationDetailsPayload, RegistrationAuditModel, RegistrationAuditService}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations.{SelectBusinessType, TypeOfPartnership}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualData
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.AddressPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.{DateOfBirthRegPage, WhatsYourContactNumberPage, WhatsYourEmailAddressPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.*

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationAuditServiceSpec extends SpecBase with MockitoSugar with Matchers with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val service = new RegistrationAuditService(
    auditConnector = mockAuditConnector
  )
  val started: Option[Instant] = Some(Instant.parse("2025-01-01T10:00:00Z"))

  override def afterEach(): Unit = {
    reset(mockAuditConnector)
    super.afterEach()
  }

  private def mkValidIndividualData(
                                     testUserId: String = Fixtures.user,
                                     validNino: String = Fixtures.nino,
                                     first: String = Fixtures.firstName,
                                     last: String = Fixtures.lastName,
                                     cred: String = Fixtures.credId
                                   ): ValidIndividualData =
    new ValidIndividualData {
      override val userId: String = testUserId
      override val nino: String = validNino
      override val firstName: String = first
      override val lastName: String = last
      override val credId: String = cred
    }


  private def uaForOrganisation(answers: UserAnswers): UserAnswers = {
    answers
      .set(UkOrNotPage, true).success.value
      .set(SelectBusinessTypePage, SelectBusinessType.Partnership).success.value
      .set(TypeOfPartnershipPage, TypeOfPartnership.LimitedPartnership).success.value
      .set(AddressPage(), fakeAddress).success.value
      .set(ContactEmailAddressPage, "test@test.com").success.value
      .set(ContactNumberPage, "07538 511 122").success.value
  }

  private def uaForIndividual(answers: UserAnswers): UserAnswers = {
    answers
      .set(AddressPage(), fakeAddress).success.value
      .set(WhatsYourEmailAddressPage, "test@test.com").success.value
      .set(WhatsYourContactNumberPage, "07538 511 122").success.value
      .set(DateOfBirthRegPage, Fixtures.dateOfBirth).success.value
  }

  "RegistrationAuditService" - {

    "builds and sends Organisation audit model" in {

      service.auditOrganisationRegistrationComplete(
        registrationStarted = started,
        userAnswers = uaForOrganisation(emptyUserAnswers),
        credId = Fixtures.credId
      )

      val captor: ArgumentCaptor[RegistrationAuditModel[OrganisationDetailsPayload]] =
        ArgumentCaptor.forClass(classOf[RegistrationAuditModel[OrganisationDetailsPayload]])

      verify(mockAuditConnector, times(1))
        .sendExplicitAudit(
          meq("RegistrationSubmission"),
          captor.capture()
        )(meq(hc), any[ExecutionContext], any[Writes[RegistrationAuditModel[OrganisationDetailsPayload]]])

      val sent = captor.getValue

      sent.affinityGroup mustBe "Organisation"
      sent.credentialId mustBe Fixtures.credId
      sent.registrationStarted mustBe started

      val details = sent.userEnteredDetails
      details.operateInTheUk mustBe true
      details.businessType mustBe "partnership"
      details.typeOfPartnerShip mustBe Some("limitedPartnership")
      details.contactDetails.addressLine1 must not be empty
      details.contactDetails.postCode mustBe fakeAddress.address.postcode
      details.contactDetails.country mustBe fakeAddress.address.country.code
      details.contactDetails.email mustBe "test@test.com"
      details.contactDetails.telephoneNumber mustBe "07538 511 122"

    }

    "builds and sends Individual audit model" in {

      service.auditIndividualRegistrationComplete(
        registrationStarted = started,
        data = mkValidIndividualData(),
        userAnswers = uaForIndividual(emptyUserAnswers)
      )

      val captor: ArgumentCaptor[RegistrationAuditModel[IndividualDetailsPayload]] =
        ArgumentCaptor.forClass(classOf[RegistrationAuditModel[IndividualDetailsPayload]])

      verify(mockAuditConnector, times(1))
        .sendExplicitAudit(
          meq("RegistrationSubmission"),
          captor.capture()
        )(meq(hc), any[ExecutionContext], any[Writes[RegistrationAuditModel[IndividualDetailsPayload]]])

      val sent = captor.getValue

      sent.affinityGroup mustBe "Individual"
      sent.credentialId mustBe Fixtures.credId
      sent.registrationStarted mustBe started

      val details = sent.userEnteredDetails
      details.firstName mustBe Fixtures.firstName
      details.lastName mustBe Fixtures.lastName
      details.nino mustBe Fixtures.nino
      details.dateOfBirth mustBe Fixtures.formattedDateOfBirth
      details.contactDetails.addressLine1 must not be empty
      details.contactDetails.postCode mustBe fakeAddress.address.postcode
      details.contactDetails.country mustBe fakeAddress.address.country.code
      details.contactDetails.email mustBe "test@test.com"
      details.contactDetails.uprn mustBe Some("990091234505")
      details.contactDetails.telephoneNumber mustBe "07538 511 122"

    }

    "does not send Organisation audit model when builder returns None" in {

      service.auditOrganisationRegistrationComplete(
        registrationStarted = started,
        userAnswers = emptyUserAnswers,
        credId = Fixtures.credId
      )

      verify(mockAuditConnector, times(0))
        .sendExplicitAudit(any[String], any())(any[HeaderCarrier], any[ExecutionContext], any())

    }

    "does not send Individual audit model when builder returns None" in {

      service.auditIndividualRegistrationComplete(
        registrationStarted = started,
        data = mkValidIndividualData(),
        userAnswers = emptyUserAnswers
      )

      verify(mockAuditConnector, times(0))
        .sendExplicitAudit(any[String], any())(any[HeaderCarrier], any[ExecutionContext], any())
    }
  }
}

