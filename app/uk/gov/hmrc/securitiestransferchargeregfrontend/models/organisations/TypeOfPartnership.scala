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
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{Enumerable, WithName}

sealed trait TypeOfPartnership

object TypeOfPartnership extends Enumerable.Implicits {

  case object GeneralPartnership extends WithName("generalPartnership") with TypeOfPartnership
  case object ScottishPartnership extends WithName("scottishPartnership") with TypeOfPartnership
  case object ScottishLimitedPartnership extends WithName("scottishLimitedPartnership") with TypeOfPartnership
  case object LimitedPartnership extends WithName("limitedPartnership") with TypeOfPartnership
  case object LimitedLiabilityPartnership extends WithName("limitedLiabilityPartnership") with TypeOfPartnership

  val values: Seq[TypeOfPartnership] = Seq(
    GeneralPartnership, ScottishPartnership, ScottishLimitedPartnership, LimitedPartnership, LimitedLiabilityPartnership
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(messages(s"typeOfPartnership.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[TypeOfPartnership] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
