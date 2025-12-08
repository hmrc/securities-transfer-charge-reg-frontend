/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargeregfrontend.forms

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.mappings.Mappings

import java.time.LocalDate
import javax.inject.Inject

class DateOfBirthRegFormProvider @Inject() extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey     = "dateOfBirthReg.error.invalid",
        allRequiredKey = "dateOfBirthReg.error.required.all",
        twoRequiredKey = "dateOfBirthReg.error.required.two",
        requiredKey    = "dateOfBirthReg.error.required",
        futureDateKey = "dateOfBirthReg.error.futureDate",
        pastDateKey = "dateOfBirthReg.error.pastDate",
        under18DateKey = "dateOfBirthReg.error.under18")
      )
}
