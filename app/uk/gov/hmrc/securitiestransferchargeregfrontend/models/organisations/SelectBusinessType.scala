/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.securitiestransferchargeregfrontend.models.organisations

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.*

sealed trait SelectBusinessType

object SelectBusinessType extends Enumerable.Implicits {
  
  case object LimitedCompany extends WithName("limited Company") with SelectBusinessType
  case object Partnership extends WithName("Partnership") with SelectBusinessType
  case object SoleTrader extends WithName("Sole Trader") with SelectBusinessType
  case object Trust extends WithName("Trust") with SelectBusinessType
  case object RegisteredSociety extends WithName("Registered Society") with SelectBusinessType
  case object UnincorporatedAssociation extends WithName("Unincorporated Association") with SelectBusinessType

  val values: Seq[SelectBusinessType] = Seq(
    LimitedCompany, Partnership
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"selectBusinessType.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[SelectBusinessType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
