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

import javax.inject.Inject

import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.mappings.Mappings
import play.api.data.Form

class WhatsYourContactNumberFormProvider @Inject() extends Mappings {

  private final val phoneRegex = """^[A-Z0-9 )/(\-*#+]*$""".stripMargin
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
