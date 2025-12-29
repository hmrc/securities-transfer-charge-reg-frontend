package forms.organisations

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations.UkOrNotFormProvider

class UkOrNotFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "ukOrNot.error.required"
  val invalidKey = "error.boolean"

  val form = new UkOrNotFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
