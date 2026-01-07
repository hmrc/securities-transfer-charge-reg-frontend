package forms

import forms.behaviours.OptionFieldBehaviours
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.SelectBusinessType
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations.SelectBusinessTypeFormProvider

class SelectBusinessTypeFormProviderSpec extends OptionFieldBehaviours {

  val form = new SelectBusinessTypeFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "selectBusinessType.error.required"

    behave like optionsField[SelectBusinessType](
      form,
      fieldName,
      validValues  = SelectBusinessType.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
