package uk.gov.hmrc.securitiestransferchargeregfrontend.pages

import java.time.LocalDate

import play.api.libs.json.JsPath

case object $className$Page extends QuestionPage[LocalDate] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "$className;format="decap"$"
}
