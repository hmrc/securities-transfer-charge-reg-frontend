package uk.gov.hmrc.securitiestransferchargeregfrontend.forms.organisations

import play.api.data.Form
import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.mappings.Mappings

import javax.inject.Inject

class UkOrNotFormProvider @Inject() extends Mappings {

  def apply(): Form[Boolean] =
    Form(
      "value" -> boolean("ukOrNot.error.required")
    )
}
