package uk.gov.hmrc.securitiestransferchargeregfrontend.forms

import javax.inject.Inject

import uk.gov.hmrc.securitiestransferchargeregfrontend.forms.mappings.Mappings
import play.api.data.Form
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.$className$

class $className$FormProvider @Inject() extends Mappings {

  def apply(): Form[$className$] =
    Form(
      "value" -> enumerable[$className$]("$className;format="decap"$.error.required")
    )
}
