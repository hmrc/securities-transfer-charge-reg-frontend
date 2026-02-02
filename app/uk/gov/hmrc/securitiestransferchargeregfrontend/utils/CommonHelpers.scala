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

package uk.gov.hmrc.securitiestransferchargeregfrontend.utils

import play.api.Logger
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{AlfAddress, AlfConfirmedAddress, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.individuals.{DateOfBirthRegPage, IndividualAddressPage, WhatsYourContactNumberPage, WhatsYourEmailAddressPage}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.{ContactEmailAddressPage, ContactNumberPage, OrgAddressPage, UkOrNotPage}

import java.time.LocalDate
import scala.concurrent.Future

object CommonHelpers:

  val getIndividualAddress: UserAnswers => Option[AlfConfirmedAddress] = _.get[AlfConfirmedAddress](IndividualAddressPage)
  val getOrgAddress: UserAnswers => Option[AlfConfirmedAddress] = _.get[AlfConfirmedAddress](OrgAddressPage)
  val getEmailAddress: UserAnswers => Option[String] = _.get[String](WhatsYourEmailAddressPage)
  val getContactEmailAddress: UserAnswers => Option[String] = _.get[String](ContactEmailAddressPage)
  val getContactNumber: UserAnswers => Option[String] = _.get[String](ContactNumberPage)
  val getTelephoneNumber: UserAnswers => Option[String] = _.get[String](WhatsYourContactNumberPage)
  val getDateOfBirth: UserAnswers => Option[LocalDate] = _.get[LocalDate](DateOfBirthRegPage)
  val getUkOrNot: UserAnswers => Option[Boolean] = _.get[Boolean](UkOrNotPage)

  val extractLines: AlfAddress => Option[(String, Option[String], Option[String])] = { address =>
    val lines = address.lines
    lines.headOption.map { h =>
      (h, lines.lift(1), lines.lift(2))
    }
  }

  def logInfoAndFail[A, E <: Throwable](logger: Logger): E => Future[A] = e => {
    logger.info(e.getMessage)
    Future.failed(e)
  }

