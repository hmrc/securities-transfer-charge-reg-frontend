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
import org.scalacheck.Gen
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.individuals.WhatsYourContactNumberFormProvider

class WhatsYourContactNumberFormProviderSpec extends StringFieldBehaviours {
  
  val requiredKey = "whatsYourContactNumber.error.required"
  val lengthKey = "whatsYourContactNumber.error.length"
  val formatKey = "whatsYourContactNumber.error.invalid"
  val maxLength = 25

  val form = new WhatsYourContactNumberFormProvider()()

  val invalid = Seq(
    "fooexample.com",
    "@example.com",
    "foo@example"
  )
  
  val valid = Seq(
    "07649 599 833",
    "+402 773 8899",
    "0800 700 400",
    "+1 656-778733"
  )

  val tooLong = Seq(
    "07649 599 833 9087 8733 12323 88",
    "+402 773 8899 - 003 - 88883233 999",
    "(0800) 700 400 1234567890 09887765431",
    "+1 656-778733 - 777 77777 77 778888"
  )
  
  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.oneOf(valid)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "not allow invalid contact number" in {

      forAll(Gen.oneOf(invalid)) { invalid =>
        val result = form.bind(Map(fieldName -> invalid)).apply(fieldName)

        result.errors.size mustBe 1
        result.errors.head.key mustBe fieldName
        result.errors.head.message mustBe formatKey

      }
    }
    
  }
}
