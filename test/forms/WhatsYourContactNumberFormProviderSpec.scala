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

package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.WhatsYourContactNumberFormProvider
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class WhatsYourContactNumberFormProviderSpec extends StringFieldBehaviours {
  
  val requiredKey = "whatsYourContactNumber.error.required"
  val lengthKey = "whatsYourContactNumber.error.length"
  val formatKey = "whatsYourContactNumber.error.invalid"
  val maxLength = 25

  val form = new WhatsYourContactNumberFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "not allow invalid contact number" in {

      val invalids = Seq(
        "fooexample.com",
        "@example.com",
        "foo@example"
      )

      forAll(Gen.oneOf(invalids)) { invalid =>
        val result = form.bind(Map(fieldName -> invalid)).apply(fieldName)

        result.errors.size mustBe 1
        result.errors.head.key mustBe fieldName
        result.errors.head.message mustBe formatKey

      }
    }
  }
}
