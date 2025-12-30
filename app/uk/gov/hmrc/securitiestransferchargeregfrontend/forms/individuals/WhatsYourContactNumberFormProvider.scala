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

package uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals

import play.api.data.Form
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.mappings.Mappings

import javax.inject.Inject

class WhatsYourContactNumberFormProvider @Inject() extends Mappings {

  private final val ukPhoneRegex
    = """^(?:0\s?7\d{3}[ \-]?\d{6}|0\s?1\d{3}[ \-]?\d{6,7}|0\s?2\d{2}[ \-]?\d{7}|0\s?3\d{2}[ \-]?\d{7}|0800[ \-]?\d{4,6}|0\s?8\d{2}[ \-]?\d{7})$"""
  private final val internationalPhoneRegex = """^\+?[0-9]{1,3}[ \-\.]?(?:\(?[0-9]{1,4}\)?[ \-\.]?)*[0-9]{3,}$"""
  private final val phoneRegex = (s"(?:${ukPhoneRegex})|(?:${internationalPhoneRegex})")
  private val maxLength = 25


  def apply(): Form[String] =
    Form(
      "value" -> validatedText(
        "whatsYourContactNumber.error.required",
        "whatsYourContactNumber.error.invalid",
        "whatsYourContactNumber.error.length",
        phoneRegex,
        maxLength
      )
    )
}
