package uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations

import play.api.libs.json.JsPath
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.QuestionPage

case object UkOrNotPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "ukOrNot"
}
