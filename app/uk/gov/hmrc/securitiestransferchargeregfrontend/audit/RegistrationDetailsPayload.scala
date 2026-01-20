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

import play.api.libs.json.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.requests.ValidIndividualData
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.{SelectBusinessTypePage, TypeOfPartnershipPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.utils.CommonHelpers.*

sealed trait RegistrationDetailsPayload

case class ContactDetails(addressLine1: String,
                          addressLine2: Option[String],
                          addressLine3: Option[String],
                          postCode: String,
                          country: String,
                          telephoneNumber: String,
                          mobileNumber: Option[String],
                          email: String)

object ContactDetails {
  implicit val format: OFormat[ContactDetails] = Json.format[ContactDetails]
}

case class OrganisationDetailsPayload(
                                       operateInTheUk: Boolean,
                                       businessType: String,
                                       typeOfPartnerShip: Option[String],
                                       contactDetails: ContactDetails
                                     ) extends RegistrationDetailsPayload

case class IndividualDetailsPayload(
                                     firstName: String,
                                     lastName: String,
                                     dateOfBirth: String,
                                     nino: String,
                                     contactDetails: ContactDetails

                                   ) extends RegistrationDetailsPayload

object RegistrationDetailsPayload {

  def fromIndividual(data: ValidIndividualData,
                     userAnswers: UserAnswers
                    ): Option[IndividualDetailsPayload] =
    for {
      alf <- getAddress(userAnswers)
      dob <- getDateOfBirth(userAnswers)
      address = alf.address
      (l1, l2, l3) <- extractLines(address)
      email <- getEmailAddress(userAnswers)
      tel <- getTelephoneNumber(userAnswers)
    } yield IndividualDetailsPayload(firstName = data.firstName, lastName = data.lastName,
      dateOfBirth = dob.toString,
      nino = data.nino,
      contactDetails = ContactDetails(
        addressLine1 = l1,
        addressLine2 = l2,
        addressLine3 = l3,
        postCode = address.postcode,
        country = address.country.code,
        telephoneNumber = tel,
        mobileNumber = None,
        email = email
      ))

  def fromOrganisation(
                        userAnswers: UserAnswers
                      ): Option[OrganisationDetailsPayload] =
    for {
      ukOrNot <- getUkOrNot(userAnswers)
      businessType <- userAnswers.get(SelectBusinessTypePage).map(_.toString)
      alf <- getAddress(userAnswers)
      address = alf.address
      (l1, l2, l3) <- extractLines(address)
      email <- getContactEmailAddress(userAnswers)
      tel <- getContactNumber(userAnswers)
    } yield OrganisationDetailsPayload(
      operateInTheUk = ukOrNot,
      businessType = businessType,
      typeOfPartnerShip = userAnswers.get(TypeOfPartnershipPage).map(_.toString),
      contactDetails = ContactDetails(
        addressLine1 = l1,
        addressLine2 = l2,
        addressLine3 = l3,
        postCode = address.postcode,
        country = address.country.code,
        telephoneNumber = tel,
        mobileNumber = None,
        email = email
      ))

  implicit val organisationDetailsWrites: OFormat[OrganisationDetailsPayload] = Json.format[OrganisationDetailsPayload]
  implicit val individualDetailsWrites: OFormat[IndividualDetailsPayload] = Json.format[IndividualDetailsPayload]
}


