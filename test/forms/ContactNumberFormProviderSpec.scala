package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.ContactNumberFormProvider

class ContactNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "contactNumber.error.required"
  val lengthKey = "contactNumber.error.length"
  val maxLength = 100

  val form = new ContactNumberFormProvider()()

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
