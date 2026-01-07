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

package uk.gov.hmrc.securitiestransferchargeregfrontend.viewmodels.checkAnswers

import uk.gov.hmrc.securitiestransferchargeregfrontend.controllers.organisations.routes.TypeOfPartnershipController
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargeregfrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargeregfrontend.pages.organisations.TypeOfPartnershipPage
import uk.gov.hmrc.securitiestransferchargeregfrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargeregfrontend.viewmodels.implicits.*

object TypeOfPartnershipSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(TypeOfPartnershipPage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"typeOfPartnership.$answer"))
          )
        )

        SummaryListRowViewModel(
          key = "typeOfPartnership.checkYourAnswersLabel",
          value = value,
          actions = Seq(
            ActionItemViewModel("site.change", TypeOfPartnershipController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("typeOfPartnership.change.hidden"))
          )
        )
    }
}
