package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class WhatsYourContactNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "whatsYourContactNumber.error.required"
  val lengthKey = "whatsYourContactNumber.error.length"
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
  }
}
