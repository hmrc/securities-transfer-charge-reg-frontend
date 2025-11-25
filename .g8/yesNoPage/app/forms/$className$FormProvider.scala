package uk.gov.hmrc.securitiestransferchargeregfrontend.forms

import javax.inject.Inject

import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.mappings.Mappings
import play.api.data.Form

class $className$FormProvider @Inject() extends Mappings {

  def apply(): Form[Boolean] =
    Form(
      "value" -> boolean("$className;format="decap"$.error.required")
    )
}
