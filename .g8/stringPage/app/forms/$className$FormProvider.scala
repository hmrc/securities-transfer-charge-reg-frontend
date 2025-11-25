package uk.gov.hmrc.securitiestransferchargeregfrontend.forms

import javax.inject.Inject

import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.mappings.Mappings
import play.api.data.Form

class $className$FormProvider @Inject() extends Mappings {

  def apply(): Form[String] =
    Form(
      "value" -> text("$className;format="decap"$.error.required")
        .verifying(maxLength($maxLength$, "$className;format="decap"$.error.length"))
    )
}
