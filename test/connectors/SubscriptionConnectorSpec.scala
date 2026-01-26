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

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargeregfrontend.audit.{OrganisationDetailsPayload, RegistrationAuditService}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.EnrolmentResponse.EnrolmentSuccessful
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.SubscriptionResponse.{SubscriptionFailed, SubscriptionSuccessful}
import uk.gov.hmrc.securitiestransferchargeregfrontend.clients.registration.{IndividualEnrolmentDetails, IndividualSubscriptionDetails, OrganisationEnrolmentDetails, OrganisationSubscriptionDetails, RegistrationClient, SubscriptionResult, SubscriptionServerError}
import uk.gov.hmrc.securitiestransferchargeregfrontend.connectors.{RegistrationDataNotFoundException, SubscriptionConnector, SubscriptionConnectorImpl, SubscriptionErrorException}
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualData
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{AlfAddress, AlfConfirmedAddress, Country, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.AddressPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.{WhatsYourContactNumberPage, WhatsYourEmailAddressPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.{ContactEmailAddressPage, ContactNumberPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.repositories.{RegistrationData, RegistrationDataRepository}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class SubscriptionConnectorSpec extends SpecBase with MockitoSugar with ScalaFutures {
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  
  private val testId = "test-id"
  private val testSafeId = "SAFE123456789"
  private val testSubscriptionId = "SUBSCRIPTION123456789"
  private val testUtr = "1234567890"
  private val testEmail = "foo@bar.com"
  private val testTelephoneNumber = "01234567890"
  private val testAuditRef = "test-audit-ref"
  private val testNino = "AA123456A"
  private val testCredId = "test-cred-id"
  
  private val testAddress: AlfAddress = AlfAddress(
    lines = List("Line 1", "Line 2"),
    postcode = "TE1 1ST",
    country = Country("GB", "United Kingdom")
  )
  val testAlfConfirmedAddress: AlfConfirmedAddress = AlfConfirmedAddress(
    auditRef = testAuditRef,
    address = testAddress,
    id = Some(testId)
  )

  private val testRegData = RegistrationData(
    id = testId,
    safeId = Some(testSafeId),
    subscriptionId = Some(testSubscriptionId),
    ctUtr = Some(testUtr),
    lastUpdated = Instant.now,
    startedAt = Some(Instant.now)
  )

  private def testUserAnswersForIndividuals(email: Option[String] = Some(testEmail),
                                            tel: Option[String] = Some(testTelephoneNumber),
                                            address: Option[AlfConfirmedAddress] = Some(testAlfConfirmedAddress)): UserAnswers = {
    val answers = UserAnswers.empty(testId)
    for {
      a1 <- email.fold(Success(answers))(answers.set(WhatsYourEmailAddressPage, _))
      a2 <- tel.fold(Success(a1))(a1.set(WhatsYourContactNumberPage, _))
      a3 <- address.fold(Success(a2))(a2.set(AddressPage(), _))
    } yield a3
  }.get

  private def testUserAnswersForOrganisation( email: Option[String] = Some(testEmail),
                                              tel: Option[String] = Some(testTelephoneNumber),
                                              address: Option[AlfConfirmedAddress] = Some(testAlfConfirmedAddress)): UserAnswers = {
    val answers = UserAnswers.empty(testId)
    for {
      a1 <- email.fold(Success(answers))(answers.set(ContactEmailAddressPage, _))
      a2 <- tel.fold(Success(a1))(a1.set(ContactNumberPage, _))
      a3 <- address.fold(Success(a2))(a2.set(AddressPage(), _))
    } yield a3
  }.get

  val testValidIndividualData: ValidIndividualData = new ValidIndividualData {
    override def firstName: String = "First"
    override def lastName: String = "Last"
    override def nino: String = testNino
    override def credId: String = "cred-id"
    override def userId: String = testId
  }
   

  def testSetupForIndividuals(regData: Option[RegistrationData] = Some(testRegData),
                              subscriptionResult: SubscriptionResult = Right(SubscriptionSuccessful(testSubscriptionId))): SubscriptionConnector = {
    val regClient = mock[RegistrationClient]
    val regRepo = mock[RegistrationDataRepository]
    val regAudit = mock[RegistrationAuditService]

    when(regRepo.getRegistrationData(testId)).thenReturn(regData.fold(Future.failed(new RegistrationDataNotFoundException("No registration data found")))(Future.successful))
    when(regRepo.setSubscriptionId(testId)(testSubscriptionId)).thenReturn(Future.successful(()))
    when(regClient.subscribe(any[IndividualSubscriptionDetails])(any[HeaderCarrier])).thenReturn(Future.successful(subscriptionResult))
    when(regClient.enrolIndividual(any[IndividualEnrolmentDetails])(any[HeaderCarrier])).thenReturn(Future.successful(Right(EnrolmentSuccessful)))
    doNothing().when(regAudit).auditIndividualRegistrationComplete(any[Option[Instant]], any[ValidIndividualData], any[UserAnswers])(any[HeaderCarrier])
    new SubscriptionConnectorImpl(regClient, regRepo, regAudit)
  }

  def testSetupForOrganisations(regData: Option[RegistrationData] = Some(testRegData),
                              subscriptionResult: SubscriptionResult = Right(SubscriptionSuccessful(testSubscriptionId))): SubscriptionConnector = {
    val regClient = mock[RegistrationClient]
    val regRepo = mock[RegistrationDataRepository]
    val regAudit = mock[RegistrationAuditService]

    when(regRepo.getRegistrationData(testId)).thenReturn(regData.fold(Future.failed(new RegistrationDataNotFoundException("No registration data found")))(Future.successful))
    when(regRepo.setSubscriptionId(testId)(testSubscriptionId)).thenReturn(Future.successful(()))
    when(regClient.subscribe(any[OrganisationSubscriptionDetails])(any[HeaderCarrier])).thenReturn(Future.successful(subscriptionResult))
    when(regClient.enrolOrganisation(any[OrganisationEnrolmentDetails])(any[HeaderCarrier])).thenReturn(Future.successful(Right(EnrolmentSuccessful)))
    doNothing().when(regAudit).auditOrganisationRegistrationComplete(any[Option[Instant]], any[UserAnswers], any[String])(any[HeaderCarrier])
    new SubscriptionConnectorImpl(regClient, regRepo, regAudit)
  }

  "The Subscription Connector" - {

    "when subscribing and enrolling individuals" - {

      "fail if getting the registration data fails" in {
        val connector = testSetupForIndividuals(regData = None)
        val result = connector.subscribeAndEnrolIndividual(testId)(testUserAnswersForIndividuals(), testValidIndividualData)

        whenReady(result.failed) { ex =>
          ex mustBe a[RegistrationDataNotFoundException]
        }
      }

      "fail if the registration data has no safe id" in {
        val regData = testRegData.copy(safeId = None)
        val connector = testSetupForIndividuals(regData = Some(regData))
        val result = connector.subscribeAndEnrolIndividual(testId)(testUserAnswersForIndividuals(), testValidIndividualData)
        
        whenReady(result.failed) { ex =>
          ex mustBe a[RegistrationDataNotFoundException]
        }
      }

      "fail if the user answers has no address" in {
        val uaNoAddress = testUserAnswersForIndividuals(address = None)
        val connector = testSetupForIndividuals()
        val result = connector.subscribeAndEnrolIndividual(testId)(uaNoAddress, testValidIndividualData)
        whenReady(result.failed) { ex =>
          ex mustBe a[RegistrationDataNotFoundException]
        }
      }

      "fail if the user answers has no email address" in {
        val uaNoEmail = testUserAnswersForIndividuals(email = None)
        val connector = testSetupForIndividuals()
        val result = connector.subscribeAndEnrolIndividual(testId)(uaNoEmail, testValidIndividualData)
        whenReady(result.failed) { ex =>
          ex mustBe a[RegistrationDataNotFoundException]
        }
      }

      "fail if the user answers has no telephone number" in {
        val uaNoTel = testUserAnswersForIndividuals(tel = None)
        val connector = testSetupForIndividuals()
        val result = connector.subscribeAndEnrolIndividual(testId)(uaNoTel, testValidIndividualData)
        whenReady(result.failed) { ex =>
          ex mustBe a[RegistrationDataNotFoundException]
        }
      }

      "fail if the registration client returns SubscriptionFailed" in {
        val connector = testSetupForIndividuals(subscriptionResult = Right(SubscriptionFailed))
        val result = connector.subscribeAndEnrolIndividual(testId)(testUserAnswersForIndividuals(), testValidIndividualData)
        whenReady(result.failed) { ex =>
          ex mustBe a[SubscriptionErrorException]
        }
      }

      "fail if the registration client returns a RegistrationServiceError" in {
        val connector = testSetupForIndividuals(subscriptionResult = Left(SubscriptionServerError("Error")))
        val result = connector.subscribeAndEnrolIndividual(testId)(testUserAnswersForIndividuals(), testValidIndividualData)
        whenReady(result.failed) { ex =>
          ex mustBe a[SubscriptionErrorException]
        }
      }

      "succeed if all data is present and the registration client returns SubscriptionSuccessful" in {
        val connector = testSetupForIndividuals()
        val result = connector.subscribeAndEnrolIndividual(testId)(testUserAnswersForIndividuals(), testValidIndividualData)

        whenReady(result) { result =>
          result mustBe ()
        }
      }

    }
    "when subscribing and enrolling organisations" - {

      "fail if getting the registration data fails" in {
        val connector = testSetupForOrganisations(regData = None)
        val result = connector.subscribeAndEnrolOrganisation(testId, testCredId)(testUserAnswersForOrganisation())

        whenReady(result.failed) { ex =>
          ex mustBe a[RegistrationDataNotFoundException]
        }
      }

      "fail if the registration data has no safe id" in {
        val regData = testRegData.copy(safeId = None)
        val connector = testSetupForOrganisations(regData = Some(regData))
        val result = connector.subscribeAndEnrolOrganisation(testId, testCredId)(testUserAnswersForOrganisation())

        whenReady(result.failed) { ex =>
          ex mustBe a[RegistrationDataNotFoundException]
        }
      }

      "fail if the user answers has no address" in {
        val uaNoAddress = testUserAnswersForOrganisation(address = None)
        val connector = testSetupForOrganisations()
        val result = connector.subscribeAndEnrolOrganisation(testId, testCredId)((uaNoAddress))
        whenReady(result.failed) { ex =>
          ex mustBe a[RegistrationDataNotFoundException]
        }
      }

      "fail if the user answers has no email address" in {
        val uaNoEmail = testUserAnswersForOrganisation(email = None)
        val connector = testSetupForOrganisations()
        val result = connector.subscribeAndEnrolOrganisation(testId, testCredId)((uaNoEmail))
        whenReady(result.failed) { ex =>
          ex mustBe a[RegistrationDataNotFoundException]
        }
      }

      "fail if the user answers has no telephone number" in {
        val uaNoTel = testUserAnswersForOrganisation(tel = None)
        val connector = testSetupForOrganisations()
        val result = connector.subscribeAndEnrolOrganisation(testId, testCredId)(uaNoTel)
        whenReady(result.failed) { ex =>
          ex mustBe a[RegistrationDataNotFoundException]
        }
      }

      "fail if the registration client returns SubscriptionFailed" in {
        val connector = testSetupForOrganisations(subscriptionResult = Right(SubscriptionFailed))
        val result = connector.subscribeAndEnrolOrganisation(testId, testCredId)(testUserAnswersForOrganisation())
        whenReady(result.failed) { ex =>
          ex mustBe a[SubscriptionErrorException]
        }
      }

      "fail if the registration client returns a RegistrationServiceError" in {
        val connector = testSetupForOrganisations(subscriptionResult = Left(SubscriptionServerError("Error")))
        val result = connector.subscribeAndEnrolOrganisation(testId, testCredId)(testUserAnswersForOrganisation())
        whenReady(result.failed) { ex =>
          ex mustBe a[SubscriptionErrorException]
        }
      }

      "succeed if all data is present and the registration client returns SubscriptionSuccessful" in {
        val connector = testSetupForOrganisations()
        val result = connector.subscribeAndEnrolOrganisation(testId, testCredId)(testUserAnswersForOrganisation())

        whenReady(result) { result =>
          result mustBe()
        }
      }

    }
  }
}

